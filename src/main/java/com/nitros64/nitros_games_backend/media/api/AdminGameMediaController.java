package com.nitros64.nitros_games_backend.media.api;

import java.io.IOException;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nitros64.nitros_games_backend.media.api.dto.GameMediaMetadataRequest;
import com.nitros64.nitros_games_backend.media.api.dto.GameMediaResponse;
import com.nitros64.nitros_games_backend.media.api.dto.GameMediaUploadRequest;
import com.nitros64.nitros_games_backend.media.api.dto.ScreenshotOrderRequest;
import com.nitros64.nitros_games_backend.media.application.GameMediaService;
import com.nitros64.nitros_games_backend.media.domain.GameMediaType;
import com.nitros64.nitros_games_backend.media.infrastructure.GameMediaProperties;
import com.nitros64.nitros_games_backend.shared.api.ApiResponse;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

@RestController
@Validated
@RequestMapping("/api/v1/admin/games/{gameId}/media")
public class AdminGameMediaController {
    private final GameMediaService service;
    private final GameMediaProperties properties;

    public AdminGameMediaController(GameMediaService service, GameMediaProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<List<GameMediaResponse>> list(@PathVariable @Positive Long gameId) {
        return ResponseEntity.ok().header("Cache-Control", "no-store, private")
                .body(service.list(gameId, true).stream().map(image -> GameMediaResponse.from(image, true)).toList());
    }

    @GetMapping("/{mediaId}/content")
    public ResponseEntity<byte[]> content(@PathVariable @Positive Long gameId, @PathVariable @Positive Long mediaId) {
        return GameMediaHttp.content(service.read(gameId, mediaId, true));
    }

    @PostMapping(path = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GameMediaResponse> createCover(@PathVariable @Positive Long gameId,
            @Valid @ModelAttribute GameMediaUploadRequest request) {
        return created(upload(gameId, GameMediaType.COVER, false, request));
    }

    @PutMapping(path = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GameMediaResponse> replaceCover(@PathVariable @Positive Long gameId,
            @Valid @ModelAttribute GameMediaUploadRequest request) {
        return ResponseEntity.ok(upload(gameId, GameMediaType.COVER, true, request));
    }

    @DeleteMapping("/cover")
    public ResponseEntity<Void> deleteCover(@PathVariable @Positive Long gameId) {
        service.deleteCover(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/screenshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GameMediaResponse> addScreenshot(@PathVariable @Positive Long gameId,
            @Valid @ModelAttribute GameMediaUploadRequest request) {
        return created(upload(gameId, GameMediaType.SCREENSHOT, false, request));
    }

    @PutMapping(path = "/{mediaId}/metadata", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameMediaResponse> metadata(@PathVariable @Positive Long gameId,
            @PathVariable @Positive Long mediaId, @Valid @RequestBody GameMediaMetadataRequest request) {
        return ResponseEntity.ok(GameMediaResponse.from(
                service.updateMetadata(gameId, mediaId, request.altText(), request.caption()), true));
    }

    @PutMapping(path = "/screenshots/order", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GameMediaResponse>> reorder(@PathVariable @Positive Long gameId,
            @Valid @RequestBody ScreenshotOrderRequest request) {
        return ResponseEntity.ok(service.reorder(gameId, request.screenshotIds()).stream()
                .map(image -> GameMediaResponse.from(image, true)).toList());
    }

    @DeleteMapping("/screenshots/{mediaId}")
    public ResponseEntity<Void> deleteScreenshot(@PathVariable @Positive Long gameId,
            @PathVariable @Positive Long mediaId) {
        service.deleteScreenshot(gameId, mediaId);
        return ResponseEntity.noContent().build();
    }

    private GameMediaResponse upload(Long gameId, GameMediaType type, boolean replace, GameMediaUploadRequest request) {
        byte[] content;
        try (var input = request.file().getInputStream()) {
            if (request.file().getSize() > properties.getMaxBytes()) {
                throw new UploadImageException("Image exceeds the configured size limit", null, HttpStatus.CONTENT_TOO_LARGE);
            }
            content = input.readNBytes(Math.toIntExact(properties.getMaxBytes() + 1));
        } catch (IOException exception) {
            throw new UploadImageException("Could not read uploaded image", exception, HttpStatus.BAD_REQUEST);
        }
        return GameMediaResponse.from(service.upload(gameId, type, replace, request.file().getOriginalFilename(),
                request.file().getContentType(), content, request.altText(), request.caption()), true);
    }

    private ResponseEntity<GameMediaResponse> created(GameMediaResponse response) {
        return ApiResponse.created(response, response.contentUrl());
    }
}
