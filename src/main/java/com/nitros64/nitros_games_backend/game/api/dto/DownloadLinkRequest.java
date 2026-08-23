package com.nitros64.nitros_games_backend.game.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DownloadLinkRequest(
        @NotBlank @Size(min = 4, max = 100) String link,
        @NotNull @Positive Long serverHostImageId) {
}
