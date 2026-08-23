package com.nitros64.nitros_games_backend.game.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GameVersionRequest(
        @NotBlank @Size(min = 4, max = 30) String name,
        @NotNull @Positive Long programmingLanguageId,
        @NotNull @Positive Long programmingToolId,
        @NotNull @Positive Long platformId,
        @NotNull @Positive Long processorId) {
}
