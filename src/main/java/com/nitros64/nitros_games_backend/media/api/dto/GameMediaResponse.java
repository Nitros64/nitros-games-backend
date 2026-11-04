package com.nitros64.nitros_games_backend.media.api.dto;

import com.nitros64.nitros_games_backend.media.application.GameMediaDetails;
import com.nitros64.nitros_games_backend.media.domain.GameMediaType;

public record GameMediaResponse(Long id, Long gameId, GameMediaType type, int position,
        String altText, String caption, String contentType, long byteSize, int width, int height,
        String contentUrl) {

    public static GameMediaResponse from(GameMediaDetails image, boolean administrative) {
        String prefix = administrative ? "/api/v1/admin/games/" : "/api/v1/games/";
        return new GameMediaResponse(image.id(), image.gameId(), image.type(), image.position(),
                image.altText(), image.caption(), image.contentType(), image.byteSize(), image.width(), image.height(),
                prefix + image.gameId() + "/media/" + image.id() + "/content");
    }
}
