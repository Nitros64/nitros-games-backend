package com.nitros64.nitros_games_backend.media.api.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ScreenshotOrderRequest(@NotNull List<@NotNull @Positive Long> screenshotIds) {}
