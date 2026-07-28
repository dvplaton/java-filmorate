package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final Map<Long, User> users = new LinkedHashMap<>();
    private long lastId = 0L;

    @GetMapping
    public Collection<User> findAll() {
        log.debug("Запрошен список пользователей, всего: {}", users.size());
        return users.values();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody User user) {
        fillNameIfBlank(user);
        user.setId(generateId());
        users.put(user.getId(), user);
        log.info("Создан пользователь: {}", user);
        return user;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id пользователя должен быть указан");
        }
        User saved = users.get(user.getId());
        if (saved == null) {
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
        fillNameIfBlank(user);
        users.put(user.getId(), user);
        log.info("Обновлён пользователь: {} -> {}", saved, user);
        return user;
    }

    /** Если имя для отображения пустое — используем логин. */
    private void fillNameIfBlank(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private long generateId() {
        return ++lastId;
    }
}