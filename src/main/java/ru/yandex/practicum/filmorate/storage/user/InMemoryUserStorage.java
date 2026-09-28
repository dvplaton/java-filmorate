package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private long lastId = 0L;

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public User create(User user) {
        user.setId(generateId());
        users.put(user.getId(), user);
        log.debug("Сохранён пользователь id={}", user.getId());
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        log.debug("Обновлён пользователь id={}", user.getId());
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return users.containsKey(id);
    }

    private long generateId() {
        return ++lastId;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        User user = users.get(userId);
        if (user != null) {
            user.getFriends().add(friendId);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        User user = users.get(userId);
        if (user != null) {
            user.getFriends().remove(friendId);
        }
    }

    @Override
    public Set<Long> getFriendIds(Long userId) {
        User user = users.get(userId);
        return user != null ? new HashSet<>(user.getFriends()) : new HashSet<>();
    }
}