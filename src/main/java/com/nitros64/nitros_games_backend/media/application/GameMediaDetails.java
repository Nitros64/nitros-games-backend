package com.nitros64.nitros_games_backend.media.application;

import com.nitros64.nitros_games_backend.media.domain.GameMedia;
import com.nitros64.nitros_games_backend.media.domain.GameMediaType;

public record GameMediaDetails(Long id, Long gameId, GameMediaType type, int position,
        String altText, String caption, String contentType, long byteSize, int width, int height) {

    public static GameMediaDetails from(GameMedia media) {
        return new GameMediaDetails(media.getId(), media.getGame().getId(), media.getType(), media.getPosition(),
                media.getAltText(), media.getCaption(), media.getContentType(), media.getByteSize(),
                media.getWidth(), media.getHeight());
    }
}
