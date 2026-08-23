package com.nitros64.nitros_games_backend.tooling.application;

import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;
import com.nitros64.nitros_games_backend.tooling.domain.ToolPlatform;
import com.nitros64.nitros_games_backend.tooling.domain.ToolProcessor;

public record ToolCompatibility(
        LanguageTool languageTool,
        ToolPlatform toolPlatform,
        ToolProcessor toolProcessor) {
}
