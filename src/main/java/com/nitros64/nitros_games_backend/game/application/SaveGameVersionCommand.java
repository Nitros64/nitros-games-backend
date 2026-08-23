package com.nitros64.nitros_games_backend.game.application;

public record SaveGameVersionCommand(
        String name,
        Long programmingLanguageId,
        Long programmingToolId,
        Long platformId,
        Long processorId) {
}
