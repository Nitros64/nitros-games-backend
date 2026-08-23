package com.nitros64.nitros_games_backend.game.api.dto;

public record DownloadLinkResponse(
        Long id,
        Long gameVersionId,
        String link,
        Long serverHostImageId) {
}
