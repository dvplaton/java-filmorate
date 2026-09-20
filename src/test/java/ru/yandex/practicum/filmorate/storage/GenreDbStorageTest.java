package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(GenreDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;

    @Test
    void testFindAll() {
        Collection<Genre> genres = genreStorage.findAll();
        assertThat(genres).isNotEmpty();
        assertThat(genres).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void testFindById() {
        Optional<Genre> genre = genreStorage.findById(1);
        assertThat(genre)
                .isPresent()
                .hasValueSatisfying(g -> {
                    assertThat(g.getId()).isEqualTo(1);
                    assertThat(g.getName()).isEqualTo("Комедия");
                });
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Genre> genre = genreStorage.findById(999);
        assertThat(genre).isEmpty();
    }

    @Test
    void testExistsById() {
        assertThat(genreStorage.existsById(1)).isTrue();
        assertThat(genreStorage.existsById(999)).isFalse();
    }
}