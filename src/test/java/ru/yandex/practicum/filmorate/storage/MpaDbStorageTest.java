package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(MpaDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class MpaDbStorageTest {

    private final MpaDbStorage mpaStorage;

    @Test
    void testFindAll() {
        Collection<Mpa> mpaList = mpaStorage.findAll();
        assertThat(mpaList).hasSize(5);
    }

    @Test
    void testFindById() {
        Optional<Mpa> mpa = mpaStorage.findById(1);
        assertThat(mpa)
                .isPresent()
                .hasValueSatisfying(m -> {
                    assertThat(m.getId()).isEqualTo(1);
                    assertThat(m.getName()).isEqualTo("G");
                });
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Mpa> mpa = mpaStorage.findById(999);
        assertThat(mpa).isEmpty();
    }

    @Test
    void testExistsById() {
        assertThat(mpaStorage.existsById(1)).isTrue();
        assertThat(mpaStorage.existsById(999)).isFalse();
    }
}