package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Mpa> MPA_MAPPER = (rs, rowNum) ->
            Mpa.builder()
                    .id(rs.getInt("id"))
                    .name(rs.getString("name"))
                    .build();

    @Override
    public Collection<Mpa> findAll() {
        return jdbc.query("SELECT id, name FROM mpa ORDER BY id", MPA_MAPPER);
    }

    @Override
    public Optional<Mpa> findById(Integer id) {
        var list = jdbc.query("SELECT id, name FROM mpa WHERE id = ?", MPA_MAPPER, id);
        return list.stream().findFirst();
    }

    @Override
    public boolean existsById(Integer id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM mpa WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }
}