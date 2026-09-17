package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Primary
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Film> FILM_MAPPER = (rs, rowNum) -> {
        Film film = Film.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date").toLocalDate())
                .duration(rs.getInt("duration"))
                .likes(new HashSet<>())
                .genres(new LinkedHashSet<>())
                .build();
        Integer mpaId = rs.getObject("mpa_id", Integer.class);
        if (mpaId != null) {
            film.setMpa(Mpa.builder().id(mpaId).name(rs.getString("mpa_name")).build());
        }
        return film;
    };

    @Override
    public Collection<Film> findAll() {
        String sql = """
                SELECT f.*, m.name AS mpa_name
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                ORDER BY f.id
                """;
        List<Film> films = jdbc.query(sql, FILM_MAPPER);
        films.forEach(this::loadGenresAndLikes);
        return films;
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Integer mpaId = film.getMpa() != null ? film.getMpa().getId() : null;
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            if (mpaId != null) {
                ps.setInt(5, mpaId);
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            film.setId(key.longValue());
        }
        saveGenres(film);
        log.debug("Создан фильм id={}", film.getId());
        return findById(film.getId()).orElse(film);
    }

    @Override
    public Film update(Film film) {
        Integer mpaId = film.getMpa() != null ? film.getMpa().getId() : null;
        jdbc.update(
                "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?",
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                mpaId,
                film.getId()
        );
        jdbc.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        saveGenres(film);
        log.debug("Обновлён фильм id={}", film.getId());
        return findById(film.getId()).orElse(film);
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sql = """
                SELECT f.*, m.name AS mpa_name
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                WHERE f.id = ?
                """;
        List<Film> list = jdbc.query(sql, FILM_MAPPER, id);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        Film film = list.get(0);
        loadGenresAndLikes(film);
        return Optional.of(film);
    }

    @Override
    public boolean existsById(Long id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM films WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public void addLike(Long filmId, Long userId) {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM likes WHERE film_id = ? AND user_id = ?",
                Integer.class, filmId, userId
        );
        if (exists == null || exists == 0) {
            jdbc.update("INSERT INTO likes (film_id, user_id) VALUES (?, ?)", filmId, userId);
        }
    }

    public void removeLike(Long filmId, Long userId) {
        jdbc.update("DELETE FROM likes WHERE film_id = ? AND user_id = ?", filmId, userId);
    }

    public Collection<Film> findPopular(int count) {
        String sql = """
                SELECT f.*, m.name AS mpa_name, COUNT(l.user_id) AS likes_count
                FROM films f
                LEFT JOIN mpa m ON f.mpa_id = m.id
                LEFT JOIN likes l ON f.id = l.film_id
                GROUP BY f.id
                ORDER BY likes_count DESC, f.id
                LIMIT ?
                """;
        List<Film> films = jdbc.query(sql, FILM_MAPPER, count);
        films.forEach(this::loadGenresAndLikes);
        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        Set<Integer> genreIds = film.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Integer genreId : genreIds) {
            jdbc.update("INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                    film.getId(), genreId);
        }
    }

    private void loadGenresAndLikes(Film film) {
        List<Genre> genres = jdbc.query(
                """
                SELECT g.id, g.name
                FROM genres g
                JOIN film_genres fg ON g.id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.id
                """,
                (rs, rowNum) -> Genre.builder()
                        .id(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .build(),
                film.getId()
        );
        film.setGenres(new LinkedHashSet<>(genres));

        List<Long> likeUserIds = jdbc.query(
                "SELECT user_id FROM likes WHERE film_id = ?",
                (rs, rowNum) -> rs.getLong("user_id"),
                film.getId()
        );
        film.setLikes(new HashSet<>(likeUserIds));
    }
}