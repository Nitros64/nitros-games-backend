package com.nitros64.nitros_games_backend.tooling.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.tooling.api.dto.LanguageToolResponse;
import com.nitros64.nitros_games_backend.tooling.domain.LanguageTool;

@Component
public class LanguageToolApiMapper {

    public LanguageToolResponse toResponse(LanguageTool association) {
        return new LanguageToolResponse(
                association.getProgrammingLanguage().getId(), association.getProgrammingTool().getId());
    }
}
