package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Primary
@RequiredArgsConstructor
@Slf4j
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbc;

    private static final RowMapper<User> USER_MAPPER = (rs, rowNum) ->
            User.builder()
                    .id(rs.getLong("id"))
                    .email(rs.getString("email"))
                    .login(rs.getString("login"))
                    .name(rs.getString("name"))
                    .birthday(rs.getDate("birthday").toLocalDate())
                    .friends(new HashSet<>())
                    .build();

    @Override
    public Collection<User> findAll() {
        List<User> users = jdbc.query("SELECT * FROM users ORDER BY id", USER_MAPPER);
        users.forEach(this::loadFriends);
        return users;
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }
        log.debug("Создан пользователь id={}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        jdbc.update(
                "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?",
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId()
        );
        log.debug("Обновлён пользователь id={}", user.getId());
        return findById(user.getId()).orElse(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        List<User> list = jdbc.query("SELECT * FROM users WHERE id = ?", USER_MAPPER, id);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        User user = list.get(0);
        loadFriends(user);
        return Optional.of(user);
    }

    @Override
    public boolean existsById(Long id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private void loadFriends(User user) {
        List<Long> friendIds = jdbc.query(
                "SELECT friend_id FROM friendship WHERE user_id = ?",
                (rs, rowNum) -> rs.getLong("friend_id"),
                user.getId()
        );
        user.setFriends(new HashSet<>(friendIds));
    }

    public void addFriend(Long userId, Long friendId) {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM friendship WHERE user_id = ? AND friend_id = ?",
                Integer.class, userId, friendId
        );
        if (exists == null || exists == 0) {
            jdbc.update(
                    "INSERT INTO friendship (user_id, friend_id, status) VALUES (?, ?, 'CONFIRMED')",
                    userId, friendId
            );
        }
    }

    public void removeFriend(Long userId, Long friendId) {
        jdbc.update("DELETE FROM friendship WHERE user_id = ? AND friend_id = ?", userId, friendId);
    }

    public Set<Long> getFriendIds(Long userId) {
        List<Long> ids = jdbc.query(
                "SELECT friend_id FROM friendship WHERE user_id = ?",
                (rs, rowNum) -> rs.getLong("friend_id"),
                userId
        );
        return new HashSet<>(ids);
    }
}