package com.nitros64.nitros_games_backend.game.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.game.api.dto.GameVersionRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionResponse;
import com.nitros64.nitros_games_backend.game.application.GameVersionDetails;
import com.nitros64.nitros_games_backend.game.application.SaveGameVersionCommand;

@Component
public class GameVersionApiMapper {

    public SaveGameVersionCommand toCommand(GameVersionRequest request) {
        return new SaveGameVersionCommand(
                request.name(), request.programmingLanguageId(), request.programmingToolId(),
                request.platformId(), request.processorId());
    }

    public GameVersionResponse toResponse(GameVersionDetails details) {
        return new GameVersionResponse(
                details.id(), details.gameId(), details.name(), details.programmingLanguageId(),
                details.programmingToolId(), details.platformId(), details.processorId());
    }
}
