package com.nitros64.nitros_games_backend.catalog.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.catalog.api.dto.PlatformRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.PlatformResponse;
import com.nitros64.nitros_games_backend.catalog.domain.Platform;

@Component
public class PlatformApiMapper {

    public Platform toEntity(PlatformRequest request) {
        return new Platform(request.name());
    }

    public PlatformResponse toResponse(Platform entity) {
        return new PlatformResponse(entity.getId(), entity.getName());
    }
}
