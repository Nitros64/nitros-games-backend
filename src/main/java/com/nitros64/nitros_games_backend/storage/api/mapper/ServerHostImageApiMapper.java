package com.nitros64.nitros_games_backend.storage.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.storage.api.dto.ServerHostImageResponse;
import com.nitros64.nitros_games_backend.storage.domain.ServerHostImage;

@Component
public class ServerHostImageApiMapper {

    public ServerHostImageResponse toResponse(ServerHostImage entity) {
        return new ServerHostImageResponse(
                entity.getId(),
                entity.getName(),
                entity.getImagepath());
    }
}
