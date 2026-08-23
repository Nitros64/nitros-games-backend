package com.nitros64.nitros_games_backend.storage.api.dto;

import org.springframework.web.multipart.MultipartFile;

import com.nitros64.nitros_games_backend.shared.validation.NoNumberString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServerHostImageUploadRequest(
        @NotNull(message = "fileHostImage cannot be null")
        MultipartFile fileHostImage,
        @NoNumberString
        @NotBlank(message = "No se permite campo en blanco")
        @Size(min = 4, max = 30, message = "el tamaño tiene que estar entre 4 y 30")
        String name) {
}
