package com.nitros64.nitros_games_backend.tooling.application;

import com.nitros64.nitros_games_backend.catalog.domain.Platform;
import com.nitros64.nitros_games_backend.catalog.domain.Processor;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;

public record ToolCompatibility(
        LanguageTool languageTool,
        Platform platform,
        Processor processor) {
}
