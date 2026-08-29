package com.nitros64.nitros_games_backend.game.application;

public record GameSearchCriteria(
        String name,
        Long genreId,
        Boolean jam) {

    public GameSearchCriteria {
        name = name == null || name.isBlank() ? null : name.strip();
    }
}
