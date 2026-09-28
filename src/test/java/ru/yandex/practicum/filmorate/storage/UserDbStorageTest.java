package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    @Test
    void testCreateAndFindUserById() {
        User user = User.builder()
                .email("test@example.com")
                .login("testuser")
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User created = userStorage.create(user);
        assertThat(created.getId()).isNotNull();

        Optional<User> found = userStorage.findById(created.getId());
        assertThat(found)
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getId()).isEqualTo(created.getId());
                    assertThat(u.getEmail()).isEqualTo("test@example.com");
                    assertThat(u.getLogin()).isEqualTo("testuser");
                    assertThat(u.getName()).isEqualTo("Test User");
                });
    }

    @Test
    void testUpdateUser() {
        User user = User.builder()
                .email("old@example.com")
                .login("oldlogin")
                .name("Old")
                .birthday(LocalDate.of(1985, 5, 5))
                .build();
        User created = userStorage.create(user);

        created.setEmail("new@example.com");
        created.setName("New Name");
        User updated = userStorage.update(created);

        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    void testFindAll() {
        userStorage.create(User.builder()
                .email("a@a.com").login("a").name("A").birthday(LocalDate.of(2000, 1, 1)).build());
        userStorage.create(User.builder()
                .email("b@b.com").login("b").name("B").birthday(LocalDate.of(2001, 1, 1)).build());

        assertThat(userStorage.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void testExistsById() {
        User created = userStorage.create(User.builder()
                .email("e@e.com").login("e").name("E").birthday(LocalDate.of(1995, 1, 1)).build());
        assertThat(userStorage.existsById(created.getId())).isTrue();
        assertThat(userStorage.existsById(99999L)).isFalse();
    }

    @Test
    void testAddAndRemoveFriend() {
        User u1 = userStorage.create(User.builder()
                .email("u1@t.com").login("u1").name("U1").birthday(LocalDate.of(1990, 1, 1)).build());
        User u2 = userStorage.create(User.builder()
                .email("u2@t.com").login("u2").name("U2").birthday(LocalDate.of(1991, 1, 1)).build());

        userStorage.addFriend(u1.getId(), u2.getId());

        Optional<User> found = userStorage.findById(u1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFriends()).contains(u2.getId());

        // Односторонняя: у u2 нет u1
        Optional<User> found2 = userStorage.findById(u2.getId());
        assertThat(found2).isPresent();
        assertThat(found2.get().getFriends()).doesNotContain(u1.getId());

        userStorage.removeFriend(u1.getId(), u2.getId());
        found = userStorage.findById(u1.getId());
        assertThat(found.get().getFriends()).doesNotContain(u2.getId());
    }

    @Test
    void testGetFriendIds() {
        User u1 = userStorage.create(User.builder()
                .email("u1@t.com").login("u1").name("U1")
                .birthday(LocalDate.of(1990, 1, 1)).build());
        User u2 = userStorage.create(User.builder()
                .email("u2@t.com").login("u2").name("U2")
                .birthday(LocalDate.of(1991, 1, 1)).build());

        userStorage.addFriend(u1.getId(), u2.getId());

        Set<Long> friendIds = userStorage.getFriendIds(u1.getId());
        assertThat(friendIds).contains(u2.getId());
        assertThat(userStorage.getFriendIds(u2.getId())).isEmpty();
    }
}