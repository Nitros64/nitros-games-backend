package com.nitros64.nitros_games_backend.tooling.api.dto;

import com.nitros64.nitros_games_backend.shared.validation.NoNumberString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProgramToolTypeRequest(
        @NoNumberString
        @NotBlank(message = "el campo 'name' no puede estar vacio")
        @Size(min = 4, max = 30, message = "el campo 'name' debe tener un tamaño entre 4 y 30")
        String name) {
}
