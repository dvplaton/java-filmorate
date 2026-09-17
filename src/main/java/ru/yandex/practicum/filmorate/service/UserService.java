package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserStorage userStorage;

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) {
        fillNameIfBlank(user);
        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }
        User created = userStorage.create(user);
        log.info("Создан пользователь: {}", created);
        return created;
    }

    public User update(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id пользователя должен быть указан");
        }
        User existing = getByIdOrThrow(user.getId());
        fillNameIfBlank(user);
        // сохраняем существующих друзей, если клиент их не прислал
        if (user.getFriends() == null) {
            user.setFriends(existing.getFriends() != null ? existing.getFriends() : new HashSet<>());
        }
        User updated = userStorage.update(user);
        log.info("Обновлён пользователь: {} -> {}", existing, updated);
        return updated;
    }

    public User getById(Long id) {
        return getByIdOrThrow(id);
    }

    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить себя в друзья");
        }
        User user = getByIdOrThrow(userId);
        User friend = getByIdOrThrow(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = getByIdOrThrow(userId);
        User friend = getByIdOrThrow(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Пользователи {} и {} больше не друзья", userId, friendId);
    }

    public Collection<User> getFriends(Long userId) {
        User user = getByIdOrThrow(userId);
        return user.getFriends().stream()
                .map(this::getByIdOrThrow)
                .collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        User user = getByIdOrThrow(userId);
        User other = getByIdOrThrow(otherId);

        Set<Long> commonIds = new HashSet<>(user.getFriends());
        commonIds.retainAll(other.getFriends());

        return commonIds.stream()
                .map(this::getByIdOrThrow)
                .collect(Collectors.toList());
    }

    private User getByIdOrThrow(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    private void fillNameIfBlank(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}