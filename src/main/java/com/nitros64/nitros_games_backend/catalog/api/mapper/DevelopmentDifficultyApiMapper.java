package com.nitros64.nitros_games_backend.catalog.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.catalog.api.dto.DevelopmentDifficultyRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.DevelopmentDifficultyResponse;
import com.nitros64.nitros_games_backend.catalog.domain.DevelopmentDifficulty;

@Component
public class DevelopmentDifficultyApiMapper {

    public DevelopmentDifficulty toEntity(DevelopmentDifficultyRequest request) {
        return new DevelopmentDifficulty(request.name());
    }

    public DevelopmentDifficultyResponse toResponse(DevelopmentDifficulty entity) {
        return new DevelopmentDifficultyResponse(entity.getId(), entity.getName());
    }
}
