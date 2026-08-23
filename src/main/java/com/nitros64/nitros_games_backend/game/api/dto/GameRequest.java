package com.nitros64.nitros_games_backend.game.api.dto;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GameRequest(
        @NotBlank @Size(min = 4, max = 30) String name,
        @NotBlank @Size(min = 4, max = 30) String description,
        boolean jam,
        @Positive int developerCount,
        @NotNull @Positive Long developmentDifficultyId,
        @NotEmpty Set<@Valid @NotNull @Positive Long> genreIds) {
}
