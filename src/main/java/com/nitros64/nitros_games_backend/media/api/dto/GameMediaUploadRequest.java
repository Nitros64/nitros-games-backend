package com.nitros64.nitros_games_backend.media.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record GameMediaUploadRequest(@NotNull MultipartFile file,
        @Size(max = 200) String altText, @Size(max = 500) String caption) {
    public GameMediaUploadRequest {
        altText = altText == null ? "" : altText;
        caption = caption == null ? "" : caption;
    }
}
