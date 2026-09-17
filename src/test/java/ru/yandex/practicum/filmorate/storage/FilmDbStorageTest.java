package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;

    @Test
    void testCreateAndFindFilmById() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .genres(new LinkedHashSet<>(Set.of(Genre.builder().id(1).build())))
                .build();

        Film created = filmStorage.create(film);
        assertThat(created.getId()).isNotNull();

        Optional<Film> found = filmStorage.findById(created.getId());
        assertThat(found)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getName()).isEqualTo("Test Film");
                    assertThat(f.getMpa()).isNotNull();
                    assertThat(f.getMpa().getId()).isEqualTo(1);
                    assertThat(f.getGenres()).isNotEmpty();
                });
    }

    @Test
    void testUpdateFilm() {
        Film film = Film.builder()
                .name("Old Name")
                .description("Old")
                .releaseDate(LocalDate.of(1999, 1, 1))
                .duration(90)
                .mpa(Mpa.builder().id(2).build())
                .build();
        Film created = filmStorage.create(film);

        created.setName("New Name");
        created.setDuration(100);
        Film updated = filmStorage.update(created);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getDuration()).isEqualTo(100);
    }

    @Test
    void testAddAndRemoveLike() {
        User user = userStorage.create(User.builder()
                .email("liker@t.com").login("liker").name("Liker")
                .birthday(LocalDate.of(1990, 1, 1)).build());

        Film film = filmStorage.create(Film.builder()
                .name("Liked Film")
                .description("Desc")
                .releaseDate(LocalDate.of(2010, 1, 1))
                .duration(100)
                .mpa(Mpa.builder().id(1).build())
                .build());

        filmStorage.addLike(film.getId(), user.getId());

        Optional<Film> found = filmStorage.findById(film.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLikes()).contains(user.getId());

        filmStorage.removeLike(film.getId(), user.getId());
        found = filmStorage.findById(film.getId());
        assertThat(found.get().getLikes()).doesNotContain(user.getId());
    }

    @Test
    void testFindPopular() {
        Film f1 = filmStorage.create(Film.builder()
                .name("Popular").description("d").releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100).mpa(Mpa.builder().id(1).build()).build());
        Film f2 = filmStorage.create(Film.builder()
                .name("Less").description("d").releaseDate(LocalDate.of(2001, 1, 1))
                .duration(90).mpa(Mpa.builder().id(1).build()).build());

        User u = userStorage.create(User.builder()
                .email("p@t.com").login("p").name("P").birthday(LocalDate.of(1990, 1, 1)).build());
        filmStorage.addLike(f1.getId(), u.getId());

        var popular = filmStorage.findPopular(10);
        assertThat(popular).isNotEmpty();
        assertThat(popular.iterator().next().getId()).isEqualTo(f1.getId());
    }

    @Test
    void testExistsById() {
        Film created = filmStorage.create(Film.builder()
                .name("Exists").description("d").releaseDate(LocalDate.of(2000, 1, 1))
                .duration(80).mpa(Mpa.builder().id(1).build()).build());
        assertThat(filmStorage.existsById(created.getId())).isTrue();
        assertThat(filmStorage.existsById(99999L)).isFalse();
    }
}
