package ru.yandex.practicum.filmorate.model;

/**
 * Статус дружбы между двумя пользователями.
 */
public enum FriendshipStatus {
    /** Заявка отправлена, но ещё не принята. */
    UNCONFIRMED,
    /** Дружба подтверждена обеими сторонами. */
    CONFIRMED
}