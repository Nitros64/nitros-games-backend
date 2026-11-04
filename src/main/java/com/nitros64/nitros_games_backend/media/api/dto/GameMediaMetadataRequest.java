package com.nitros64.nitros_games_backend.media.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GameMediaMetadataRequest(@NotNull @Size(max = 200) String altText,
        @NotNull @Size(max = 500) String caption) {}
