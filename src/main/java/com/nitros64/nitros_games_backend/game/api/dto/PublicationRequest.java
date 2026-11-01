package com.nitros64.nitros_games_backend.game.api.dto;

import com.nitros64.nitros_games_backend.game.domain.PublicationStatus;
import jakarta.validation.constraints.NotNull;

public record PublicationRequest(@NotNull PublicationStatus publicationStatus) {
}
