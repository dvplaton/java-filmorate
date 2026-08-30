package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * User.
 * Дружба хранится направленно: пользователь A может отправить запрос пользователю B.
 * Пока B не подтвердил — статус «неподтверждённая». После подтверждения — «подтверждённая».
 * В модели для in-memory хранения пока используются только подтверждённые друзья (Set id).
 * Полная поддержка статуса реализуется на уровне БД (таблица friendship).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    @NotBlank(message = "Электронная почта не может быть пустой")
    @Email(message = "Электронная почта должна содержать символ @ и быть корректной")
    private String email;

    @NotBlank(message = "Логин не может быть пустым")
    @Pattern(regexp = "\\S+", message = "Логин не может содержать пробелы")
    private String login;

    private String name;

    @NotNull(message = "Дата рождения должна быть указана")
    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;

    /**
     * Идентификаторы подтверждённых друзей.
     * Неподтверждённые заявки учитываются только в слое БД (таблица friendship).
     */
    @Builder.Default
    private Set<Long> friends = new HashSet<>();

}