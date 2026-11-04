package com.nitros64.nitros_games_backend.media.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.nitros64.nitros_games_backend.media.application.GameMediaService;

final class GameMediaHttp {
    private GameMediaHttp() {}

    static ResponseEntity<byte[]> content(GameMediaService.Content content) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, private")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=\"image."
                        + (content.contentType().equals("image/png") ? "png" : "jpg") + "\"")
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.bytes().length).body(content.bytes());
    }
}
