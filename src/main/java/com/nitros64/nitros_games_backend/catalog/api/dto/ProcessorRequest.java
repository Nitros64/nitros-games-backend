package com.nitros64.nitros_games_backend.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProcessorRequest(
        @NotBlank(message = "No se permite campo en blanco")
        @Size(min = 1, max = 10, message = "el tamaño tiene que estar entre 1 y 10")
        String name) {
}
