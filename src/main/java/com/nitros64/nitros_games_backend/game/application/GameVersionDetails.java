package com.nitros64.nitros_games_backend.game.application;

public record GameVersionDetails(
        Long id,
        Long gameId,
        String name,
        Long programmingLanguageId,
        Long programmingToolId,
        Long platformId,
        Long processorId) {
}
