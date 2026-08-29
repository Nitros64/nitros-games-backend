package com.nitros64.nitros_games_backend.game.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.game.api.dto.GameRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameResponse;
import com.nitros64.nitros_games_backend.game.application.GameDetails;
import com.nitros64.nitros_games_backend.game.application.SaveGameCommand;

@Component
public class GameApiMapper {

    public SaveGameCommand toCommand(GameRequest request) {
        return new SaveGameCommand(
                request.name(), request.description(), request.jam(), request.developerCount(),
                request.genreIds());
    }

    public GameResponse toResponse(GameDetails details) {
        return new GameResponse(
                details.id(), details.name(), details.description(), details.jam(),
                details.developerCount(), 
                details.genreIds());
    }

}
