package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Рейтинг Motion Picture Association (MPA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mpa {

    private Integer id;
    private String name;
}