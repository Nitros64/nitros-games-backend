package com.nitros64.nitros_games_backend.media.api;

import java.util.List;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nitros64.nitros_games_backend.media.api.dto.GameMediaResponse;
import com.nitros64.nitros_games_backend.media.application.GameMediaService;

@RestController
@Validated
@RequestMapping("/api/v1/games/{gameId}/media")
public class PublicGameMediaController {
    private final GameMediaService service;

    public PublicGameMediaController(GameMediaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<GameMediaResponse>> list(@PathVariable @Positive Long gameId) {
        return ResponseEntity.ok().header("Cache-Control", "no-store, private")
                .body(service.list(gameId, false).stream().map(image -> GameMediaResponse.from(image, false)).toList());
    }

    @GetMapping("/{mediaId}/content")
    public ResponseEntity<byte[]> content(@PathVariable @Positive Long gameId, @PathVariable @Positive Long mediaId) {
        return GameMediaHttp.content(service.read(gameId, mediaId, false));
    }
}
