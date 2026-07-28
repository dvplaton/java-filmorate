package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FilmorateApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** Bean jakarta.validation.Validator создаётся spring-boot-starter-validation. */
    @Autowired
    private Validator validator;

    // ==================== контекст ====================

    @Test
    void contextLoads() {
        assertNotNull(mockMvc);
        assertNotNull(validator);
    }

    // ==================== валидация Film ====================

    @Test
    @DisplayName("Корректный фильм проходит валидацию")
    void validFilmHasNoViolations() {
        assertTrue(validator.validate(validFilm()).isEmpty());
    }

    @Test
    @DisplayName("Пустое название фильма отклоняется")
    void blankFilmNameIsRejected() {
        Film film = validFilm().toBuilder().name("   ").build();
        assertSingleViolation(validator.validate(film), "name");
    }

    @Test
    @DisplayName("Описание длиннее 200 символов отклоняется")
    void tooLongDescriptionIsRejected() {
        Film film = validFilm().toBuilder().description("a".repeat(201)).build();
        assertSingleViolation(validator.validate(film), "description");
    }

    @Test
    @DisplayName("Описание ровно 200 символов допустимо")
    void exactlyTwoHundredCharsDescriptionIsValid() {
        Film film = validFilm().toBuilder().description("a".repeat(200)).build();
        assertTrue(validator.validate(film).isEmpty());
    }

    @Test
    @DisplayName("Дата релиза раньше 28.12.1895 отклоняется")
    void releaseDateBeforeCinemaBirthdayIsRejected() {
        Film film = validFilm().toBuilder().releaseDate(LocalDate.of(1895, 12, 27)).build();
        assertSingleViolation(validator.validate(film), "releaseDate");
    }

    @Test
    @DisplayName("Дата релиза 28.12.1895 допустима — граничное значение")
    void releaseDateOnCinemaBirthdayIsValid() {
        Film film = validFilm().toBuilder().releaseDate(LocalDate.of(1895, 12, 28)).build();
        assertTrue(validator.validate(film).isEmpty());
    }

    @Test
    @DisplayName("Отсутствие даты релиза отклоняется")
    void nullReleaseDateIsRejected() {
        Film film = validFilm().toBuilder().releaseDate(null).build();
        assertSingleViolation(validator.validate(film), "releaseDate");
    }

    @Test
    @DisplayName("Отрицательная продолжительность отклоняется")
    void negativeDurationIsRejected() {
        Film film = validFilm().toBuilder().duration(-200).build();
        assertSingleViolation(validator.validate(film), "duration");
    }

    @Test
    @DisplayName("Нулевая продолжительность отклоняется")
    void zeroDurationIsRejected() {
        Film film = validFilm().toBuilder().duration(0).build();
        assertSingleViolation(validator.validate(film), "duration");
    }

    // ==================== валидация User ====================

    @Test
    @DisplayName("Корректный пользователь проходит валидацию")
    void validUserHasNoViolations() {
        assertTrue(validator.validate(validUser()).isEmpty());
    }

    @Test
    @DisplayName("Email без символа @ отклоняется")
    void emailWithoutAtSignIsRejected() {
        User user = validUser().toBuilder().email("mail.ru").build();
        assertSingleViolation(validator.validate(user), "email");
    }

    @Test
    @DisplayName("Пустой email отклоняется")
    void blankEmailIsRejected() {
        User user = validUser().toBuilder().email("").build();
        assertTrue(validator.validate(user).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Логин с пробелом отклоняется")
    void loginWithSpaceIsRejected() {
        User user = validUser().toBuilder().login("dolore ullamco").build();
        assertSingleViolation(validator.validate(user), "login");
    }

    @Test
    @DisplayName("Пустой логин отклоняется")
    void blankLoginIsRejected() {
        User user = validUser().toBuilder().login("").build();
        assertTrue(validator.validate(user).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("login")));
    }

    @Test
    @DisplayName("Дата рождения в будущем отклоняется")
    void futureBirthdayIsRejected() {
        User user = validUser().toBuilder().birthday(LocalDate.of(2446, 8, 20)).build();
        assertSingleViolation(validator.validate(user), "birthday");
    }

    @Test
    @DisplayName("Отсутствие даты рождения отклоняется")
    void nullBirthdayIsRejected() {
        User user = validUser().toBuilder().birthday(null).build();
        assertSingleViolation(validator.validate(user), "birthday");
    }

    // ==================== эндпоинты /films ====================

    @Test
    @DisplayName("POST /films создаёт фильм и присваивает id")
    void createFilmReturnsCreatedFilmWithId() throws Exception {
        Film created = createFilm(validFilm());

        assertNotNull(created.getId());
        assertEquals("nisi eiusmod", created.getName());
        assertEquals(LocalDate.of(1967, 3, 25), created.getReleaseDate());
        assertEquals(100, created.getDuration());
    }

    @Test
    @DisplayName("POST /films с невалидными данными возвращает 400 и JSON с описанием")
    void createInvalidFilmReturnsBadRequest() throws Exception {
        Film film = validFilm().toBuilder().name("").build();

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest())
                .andExpect(content -> assertTrue(content.getResponse()
                        .getContentAsString(StandardCharsets.UTF_8).contains("name")));
    }

    @Test
    @DisplayName("PUT /films обновляет существующий фильм")
    void updateFilmReturnsUpdatedFilm() throws Exception {
        Film created = createFilm(validFilm());

        Film update = created.toBuilder()
                .name("Film Updated")
                .description("New film update decription")
                .releaseDate(LocalDate.of(1989, 4, 17))
                .duration(190)
                .build();

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.name").value("Film Updated"))
                .andExpect(jsonPath("$.description").value("New film update decription"))
                .andExpect(jsonPath("$.releaseDate").value("1989-04-17"))
                .andExpect(jsonPath("$.duration").value(190));
    }

    @Test
    @DisplayName("PUT /films с неизвестным id возвращает 404")
    void updateUnknownFilmReturnsNotFound() throws Exception {
        Film film = validFilm().toBuilder().id(9999L).build();

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /films возвращает список с созданным фильмом")
    void getAllFilmsContainsCreatedFilm() throws Exception {
        Film created = createFilm(validFilm());

        String body = mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<Film> films = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Film.class));

        assertTrue(films.stream().anyMatch(f -> f.getId().equals(created.getId())));
    }

    // ==================== эндпоинты /users ====================

    @Test
    @DisplayName("POST /users создаёт пользователя и присваивает id")
    void createUserReturnsCreatedUserWithId() throws Exception {
        User created = createUser(validUser());

        assertNotNull(created.getId());
        assertEquals("mail@mail.ru", created.getEmail());
        assertEquals("dolore", created.getLogin());
        assertEquals("Nick Name", created.getName());
        assertEquals(LocalDate.of(1946, 8, 20), created.getBirthday());
    }

    @Test
    @DisplayName("Пустое имя заменяется логином")
    void nameIsReplacedByLoginWhenBlank() throws Exception {
        User user = validUser().toBuilder()
                .email("friend@common.ru")
                .login("common")
                .name(null)
                .birthday(LocalDate.of(2000, 8, 20))
                .build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("common"))
                .andExpect(jsonPath("$.login").value("common"))
                .andExpect(jsonPath("$.email").value("friend@common.ru"))
                .andExpect(jsonPath("$.birthday").value("2000-08-20"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST /users с некорректным email возвращает 400")
    void createUserWithInvalidEmailReturnsBadRequest() throws Exception {
        User user = validUser().toBuilder().email("mail.ru").build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /users с логином, содержащим пробел, возвращает 400")
    void createUserWithSpaceInLoginReturnsBadRequest() throws Exception {
        User user = validUser().toBuilder().login("dolore ullamco").build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users с датой рождения в будущем возвращает 400")
    void createUserWithFutureBirthdayReturnsBadRequest() throws Exception {
        User user = validUser().toBuilder().birthday(LocalDate.of(2446, 8, 20)).build();

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /users обновляет существующего пользователя")
    void updateUserReturnsUpdatedUser() throws Exception {
        User created = createUser(validUser());

        User update = created.toBuilder()
                .login("doloreUpdate")
                .name("est adipisicing")
                .email("mail@yandex.ru")
                .birthday(LocalDate.of(1976, 9, 20))
                .build();

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.login").value("doloreUpdate"))
                .andExpect(jsonPath("$.name").value("est adipisicing"))
                .andExpect(jsonPath("$.email").value("mail@yandex.ru"))
                .andExpect(jsonPath("$.birthday").value("1976-09-20"));
    }

    @Test
    @DisplayName("PUT /users с неизвестным id возвращает 404")
    void updateUnknownUserReturnsNotFound() throws Exception {
        User user = validUser().toBuilder().id(9999L).build();

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /users возвращает список с созданным пользователем")
    void getAllUsersContainsCreatedUser() throws Exception {
        User created = createUser(validUser());

        String body = mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<User> users = objectMapper.readValue(body,
                objectMapper.getTypeFactory().constructCollectionType(List.class, User.class));

        assertTrue(users.stream().anyMatch(u -> u.getId().equals(created.getId())));
    }

    // ==================== вспомогательные методы ====================

    private Film validFilm() {
        return Film.builder()
                .name("nisi eiusmod")
                .description("adipisicing")
                .releaseDate(LocalDate.of(1967, 3, 25))
                .duration(100)
                .build();
    }

    private User validUser() {
        return User.builder()
                .email("mail@mail.ru")
                .login("dolore")
                .name("Nick Name")
                .birthday(LocalDate.of(1946, 8, 20))
                .build();
    }

    private Film createFilm(Film film) throws Exception {
        String body = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(body, Film.class);
    }

    private User createUser(User user) throws Exception {
        String body = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(body, User.class);
    }

    private <T> void assertSingleViolation(Set<ConstraintViolation<T>> violations, String field) {
        assertEquals(1, violations.size(), "Ожидалось ровно одно нарушение");
        assertEquals(field, violations.iterator().next().getPropertyPath().toString());
    }
}