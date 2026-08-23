package com.nitros64.nitros_games_backend.game.application;

import java.util.Set;

public record SaveGameCommand(
        String name,
        String description,
        boolean jam,
        int developerCount,
        Long developmentDifficultyId,
        Set<Long> genreIds) {
}
