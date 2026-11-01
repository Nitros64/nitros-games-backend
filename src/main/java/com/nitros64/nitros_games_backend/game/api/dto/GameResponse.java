package com.nitros64.nitros_games_backend.game.api.dto;

import java.util.Set;

import com.nitros64.nitros_games_backend.game.domain.PublicationStatus;

public record GameResponse(
        Long id,
        String name,
        String description,
        boolean jam,
        int developerCount,
        Set<Long> genreIds,
        PublicationStatus publicationStatus) {
}
