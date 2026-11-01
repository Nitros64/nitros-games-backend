package com.nitros64.nitros_games_backend.tooling.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LanguageToolRequest(
        @NotNull @Positive Long programmingLanguageId,
        @NotNull @Positive Long programmingToolId) {
}
