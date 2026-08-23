package com.nitros64.nitros_games_backend.game.api.dto;

public record GameVersionResponse(
        Long id,
        Long gameId,
        String name,
        Long programmingLanguageId,
        Long programmingToolId,
        Long platformId,
        Long processorId) {
}
