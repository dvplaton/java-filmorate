package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        validateAndEnrich(film);
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
        getByIdOrThrow(film.getId());
        validateAndEnrich(film);
        Film updated = filmStorage.update(film);
        log.info("Обновлён фильм id={}", updated.getId());
        return updated;
    }

    public Film getById(Long id) {
        return getByIdOrThrow(id);
    }

    public void addLike(Long filmId, Long userId) {
        getByIdOrThrow(filmId);
        ensureUserExists(userId);

        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        getByIdOrThrow(filmId);
        ensureUserExists(userId);

        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    public Collection<Film> getPopular(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным числом");
        }
        return filmStorage.findPopular(count);
    }

    private void validateAndEnrich(Film film) {
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            Mpa mpa = mpaStorage.findById(film.getMpa().getId())
                    .orElseThrow(() -> new NotFoundException(
                            "Рейтинг MPA с id = " + film.getMpa().getId() + " не найден"));
            film.setMpa(mpa);
        }
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> enriched = new LinkedHashSet<>();
            for (Genre g : film.getGenres()) {
                Genre genre = genreStorage.findById(g.getId())
                        .orElseThrow(() -> new NotFoundException(
                                "Жанр с id = " + g.getId() + " не найден"));
                enriched.add(genre);
            }
            film.setGenres(enriched);
        } else {
            film.setGenres(new LinkedHashSet<>());
        }
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