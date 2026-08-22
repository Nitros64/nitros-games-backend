package com.nitros64.nitros_games_backend.catalog.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.catalog.api.dto.GameGenreRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.GameGenreResponse;
import com.nitros64.nitros_games_backend.catalog.domain.GameGenre;

@Component
public class GameGenreApiMapper {

    public GameGenre toEntity(GameGenreRequest request) {
        return new GameGenre(request.name());
    }

    public GameGenreResponse toResponse(GameGenre entity) {
        return new GameGenreResponse(entity.getId(), entity.getName());
    }
}
