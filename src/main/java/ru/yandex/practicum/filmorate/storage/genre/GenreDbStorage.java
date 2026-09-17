package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Genre> GENRE_MAPPER = (rs, rowNum) ->
            Genre.builder()
                    .id(rs.getInt("id"))
                    .name(rs.getString("name"))
                    .build();

    @Override
    public Collection<Genre> findAll() {
        return jdbc.query("SELECT id, name FROM genres ORDER BY id", GENRE_MAPPER);
    }

    @Override
    public Optional<Genre> findById(Integer id) {
        var list = jdbc.query("SELECT id, name FROM genres WHERE id = ?", GENRE_MAPPER, id);
        return list.stream().findFirst();
    }

    @Override
    public boolean existsById(Integer id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM genres WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }
}