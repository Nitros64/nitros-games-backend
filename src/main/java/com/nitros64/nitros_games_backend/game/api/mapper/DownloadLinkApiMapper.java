package com.nitros64.nitros_games_backend.game.api.mapper;

import org.springframework.stereotype.Component;

import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkRequest;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkResponse;
import com.nitros64.nitros_games_backend.game.application.DownloadLinkDetails;
import com.nitros64.nitros_games_backend.game.application.SaveDownloadLinkCommand;

@Component
public class DownloadLinkApiMapper {

    public SaveDownloadLinkCommand toCommand(DownloadLinkRequest request) {
        return new SaveDownloadLinkCommand(request.link(), request.serverHostImageId());
    }

    public DownloadLinkResponse toResponse(DownloadLinkDetails details) {
        return new DownloadLinkResponse(
                details.id(), details.gameVersionId(), details.link(), details.serverHostImageId());
    }
}
