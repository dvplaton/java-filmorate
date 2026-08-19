package ru.yandex.practicum.filmorate.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReleaseDateValidator.class)
public @interface ReleaseDate {

    /** Минимально допустимая дата в формате ISO (yyyy-MM-dd). */
    String value() default "1895-12-28";

    String message() default "Некорректная дата релиза";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}