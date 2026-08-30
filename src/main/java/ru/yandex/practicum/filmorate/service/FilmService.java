package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }
        Film created = filmStorage.create(film);
        log.info("Добавлен фильм: {}", created);
        return created;
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id фильма должен быть указан");
        }
        Film existing = getByIdOrThrow(film.getId());
        if (film.getLikes() == null) {
            film.setLikes(existing.getLikes() != null ? existing.getLikes() : new HashSet<>());
        }
        Film updated = filmStorage.update(film);
        log.info("Обновлён фильм: {} -> {}", existing, updated);
        return updated;
    }

    public Film getById(Long id) {
        return getByIdOrThrow(id);
    }

    public void addLike(Long filmId, Long userId) {
        Film film = getByIdOrThrow(filmId);
        ensureUserExists(userId);

        film.getLikes().add(userId);
        filmStorage.update(film);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = getByIdOrThrow(filmId);
        ensureUserExists(userId);

        film.getLikes().remove(userId);
        filmStorage.update(film);
        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    public Collection<Film> getPopular(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным числом");
        }
        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    private Film getByIdOrThrow(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    private void ensureUserExists(Long userId) {
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }
}