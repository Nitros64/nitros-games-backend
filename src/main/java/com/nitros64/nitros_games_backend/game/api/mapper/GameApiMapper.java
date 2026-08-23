package com.nitros64.nitros_games_backend.game.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkRequest;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkResponse;
import com.nitros64.nitros_games_backend.game.api.dto.GameRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameResponse;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionResponse;
import com.nitros64.nitros_games_backend.game.application.DownloadLinkDetails;
import com.nitros64.nitros_games_backend.game.application.GameDetails;
import com.nitros64.nitros_games_backend.game.application.GameVersionDetails;
import com.nitros64.nitros_games_backend.game.application.SaveDownloadLinkCommand;
import com.nitros64.nitros_games_backend.game.application.SaveGameCommand;
import com.nitros64.nitros_games_backend.game.application.SaveGameVersionCommand;

@Component
public class GameApiMapper {

    public SaveGameCommand toCommand(GameRequest request) {
        return new SaveGameCommand(
                request.name(), request.description(), request.jam(), request.developerCount(),
                request.developmentDifficultyId(), request.genreIds());
    }

    public SaveGameVersionCommand toCommand(GameVersionRequest request) {
        return new SaveGameVersionCommand(
                request.name(), request.programmingLanguageId(), request.programmingToolId(),
                request.platformId(), request.processorId());
    }

    public SaveDownloadLinkCommand toCommand(DownloadLinkRequest request) {
        return new SaveDownloadLinkCommand(request.link(), request.serverHostImageId());
    }

    public GameResponse toResponse(GameDetails details) {
        return new GameResponse(
                details.id(), details.name(), details.description(), details.jam(),
                details.developerCount(), details.developmentDifficultyId(), details.genreIds());
    }

    public GameVersionResponse toResponse(GameVersionDetails details) {
        return new GameVersionResponse(
                details.id(), details.gameId(), details.name(), details.programmingLanguageId(),
                details.programmingToolId(), details.platformId(), details.processorId());
    }

    public DownloadLinkResponse toResponse(DownloadLinkDetails details) {
        return new DownloadLinkResponse(
                details.id(), details.gameVersionId(), details.link(), details.serverHostImageId());
    }
}
