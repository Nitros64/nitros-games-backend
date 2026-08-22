package com.nitros64.nitros_games_backend.tooling.api.dto;

import java.net.URL;

import com.nitros64.nitros_games_backend.shared.validation.NoNumberString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProgrammingToolRequest(
        @NoNumberString
        @NotBlank(message = "no puede estar vacio")
        @Size(min = 4, max = 30, message = "el tamaño tiene que estar entre 4 y 30")
        String name,
        @NotNull(message = "webPage cannot be null")
        URL webPage,
        @NotBlank(message = "no puede estar vacio")
        @Size(min = 4, max = 30, message = "el tamaño tiene que estar entre 4 y 30")
        String imagefilePath,
        @NotNull(message = "toolTypeId cannot be null")
        @Positive(message = "toolTypeId must be positive")
        Long toolTypeId) {
}
