package com.nitros64.nitros_games_backend.game.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkRequest;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkResponse;
import com.nitros64.nitros_games_backend.game.api.mapper.GameApiMapper;
import com.nitros64.nitros_games_backend.game.application.GameApplicationService;
import com.nitros64.nitros_games_backend.shared.api.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@Validated
@RequestMapping("/api/v1/games/{gameId}/versions/{versionId}/download-links")
public class DownloadLinkController {

    private final GameApplicationService service;
    private final GameApiMapper mapper;

    public DownloadLinkController(GameApplicationService service, GameApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<DownloadLinkResponse>> findAll(
            @PathVariable @Positive Long gameId,
            @PathVariable @Positive Long versionId) {
        return ResponseEntity.ok(service.findDownloadLinks(gameId, versionId).stream()
                .map(mapper::toResponse)
                .toList());
    }

    @GetMapping("/{linkId}")
    public ResponseEntity<DownloadLinkResponse> findOne(
            @PathVariable @Positive Long gameId,
            @PathVariable @Positive Long versionId,
            @PathVariable @Positive Long linkId) {
        return ResponseEntity.ok(mapper.toResponse(
                service.findDownloadLink(gameId, versionId, linkId)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DownloadLinkResponse> create(
            @PathVariable @Positive Long gameId,
            @PathVariable @Positive Long versionId,
            @Valid @RequestBody DownloadLinkRequest request) {
        var response = mapper.toResponse(
                service.createDownloadLink(gameId, versionId, mapper.toCommand(request)));
        return ApiResponse.created(
                response,
                "/api/v1/games/{gameId}/versions/{versionId}/download-links/{linkId}",
                gameId,
                versionId,
                response.id());
    }

    @PutMapping(path = "/{linkId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DownloadLinkResponse> update(
            @PathVariable @Positive Long gameId,
            @PathVariable @Positive Long versionId,
            @PathVariable @Positive Long linkId,
            @Valid @RequestBody DownloadLinkRequest request) {
        return ResponseEntity.ok(mapper.toResponse(
                service.updateDownloadLink(gameId, versionId, linkId, mapper.toCommand(request))));
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long gameId,
            @PathVariable @Positive Long versionId,
            @PathVariable @Positive Long linkId) {
        service.deleteDownloadLink(gameId, versionId, linkId);
        return ResponseEntity.noContent().build();
    }
}
