package com.nitros64.nitros_games_backend.tooling.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingLanguageRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingLanguageResponse;
import com.nitros64.nitros_games_backend.tooling.domain.ProgrammingLanguage;

@Component
public class ProgrammingLanguageApiMapper {

    public ProgrammingLanguage toEntity(ProgrammingLanguageRequest request) {
        return new ProgrammingLanguage(request.name());
    }

    public ProgrammingLanguageResponse toResponse(ProgrammingLanguage entity) {
        return new ProgrammingLanguageResponse(entity.getId(), entity.getName());
    }
}
