package com.nitros64.nitros_games_backend.tooling.api.dto;

import com.nitros64.nitros_games_backend.shared.validation.NoNumberString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProgrammingLanguageRequest(
        @NoNumberString
        @NotBlank(message = "No se permite campo en blanco")
        @Size(min = 1, max = 12, message = "el tamaño tiene que estar entre 1 y 12")
        String name) {
}
