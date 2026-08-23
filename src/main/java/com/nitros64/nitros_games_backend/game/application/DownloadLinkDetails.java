package com.nitros64.nitros_games_backend.game.application;

public record DownloadLinkDetails(
        Long id,
        Long gameVersionId,
        String link,
        Long serverHostImageId) {
}
