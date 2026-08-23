package com.nitros64.nitros_games_backend.game.api;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkRequest;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkResponse;
import com.nitros64.nitros_games_backend.game.api.dto.GameRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameResponse;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionResponse;
import com.nitros64.nitros_games_backend.game.api.mapper.GameApiMapper;
import com.nitros64.nitros_games_backend.game.application.GameApplicationService;
import com.nitros64.nitros_games_backend.game.application.GameSearchCriteria;
import com.nitros64.nitros_games_backend.shared.api.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameApplicationService service;
    private final GameApiMapper mapper;

    public GameController(GameApplicationService service, GameApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<GameResponse>> findAll() {
        return ResponseEntity.ok(service.findAllGames().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<GameResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.findAllGames(pageable), mapper::toResponse));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<GameResponse>> search(
            @RequestParam(required = false) @Size(max = 30) String name,
            @RequestParam(required = false) @Positive Long developmentDifficultyId,
            @RequestParam(required = false) @Positive Long genreId,
            @RequestParam(required = false) Boolean jam,
            Pageable pageable) {
        var criteria = new GameSearchCriteria(
                name,
                developmentDifficultyId,
                genreId,
                jam);
        return ResponseEntity.ok(PageResponse.from(
                service.searchGames(criteria, pageable),
                mapper::toResponse));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> findOne(@PathVariable Long gameId) {
        return ResponseEntity.ok(mapper.toResponse(service.findGame(gameId)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameResponse> create(@Valid @RequestBody GameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(service.createGame(mapper.toCommand(request))));
    }

    @PutMapping(path = "/{gameId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameResponse> update(
            @PathVariable Long gameId,
            @Valid @RequestBody GameRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toResponse(service.updateGame(gameId, mapper.toCommand(request))));
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> delete(@PathVariable Long gameId) {
        service.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{gameId}/versions")
    public ResponseEntity<List<GameVersionResponse>> findVersions(@PathVariable Long gameId) {
        return ResponseEntity.ok(service.findVersions(gameId).stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/{gameId}/versions/{versionId}")
    public ResponseEntity<GameVersionResponse> findVersion(
            @PathVariable Long gameId,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(mapper.toResponse(service.findVersion(gameId, versionId)));
    }

    @PostMapping(path = "/{gameId}/versions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameVersionResponse> createVersion(
            @PathVariable Long gameId,
            @Valid @RequestBody GameVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(service.createVersion(gameId, mapper.toCommand(request))));
    }

    @PutMapping(path = "/{gameId}/versions/{versionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameVersionResponse> updateVersion(
            @PathVariable Long gameId,
            @PathVariable Long versionId,
            @Valid @RequestBody GameVersionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toResponse(service.updateVersion(gameId, versionId, mapper.toCommand(request))));
    }

    @DeleteMapping("/{gameId}/versions/{versionId}")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable Long gameId,
            @PathVariable Long versionId) {
        service.deleteVersion(gameId, versionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{gameId}/versions/{versionId}/download-links")
    public ResponseEntity<List<DownloadLinkResponse>> findDownloadLinks(
            @PathVariable Long gameId,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(service.findDownloadLinks(gameId, versionId).stream()
                .map(mapper::toResponse)
                .toList());
    }

    @GetMapping("/{gameId}/versions/{versionId}/download-links/{linkId}")
    public ResponseEntity<DownloadLinkResponse> findDownloadLink(
            @PathVariable Long gameId,
            @PathVariable Long versionId,
            @PathVariable Long linkId) {
        return ResponseEntity.ok(mapper.toResponse(
                service.findDownloadLink(gameId, versionId, linkId)));
    }

    @PostMapping(
            path = "/{gameId}/versions/{versionId}/download-links",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DownloadLinkResponse> createDownloadLink(
            @PathVariable Long gameId,
            @PathVariable Long versionId,
            @Valid @RequestBody DownloadLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(
                service.createDownloadLink(gameId, versionId, mapper.toCommand(request))));
    }

    @PutMapping(
            path = "/{gameId}/versions/{versionId}/download-links/{linkId}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DownloadLinkResponse> updateDownloadLink(
            @PathVariable Long gameId,
            @PathVariable Long versionId,
            @PathVariable Long linkId,
            @Valid @RequestBody DownloadLinkRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(
                service.updateDownloadLink(gameId, versionId, linkId, mapper.toCommand(request))));
    }

    @DeleteMapping("/{gameId}/versions/{versionId}/download-links/{linkId}")
    public ResponseEntity<Void> deleteDownloadLink(
            @PathVariable Long gameId,
            @PathVariable Long versionId,
            @PathVariable Long linkId) {
        service.deleteDownloadLink(gameId, versionId, linkId);
        return ResponseEntity.noContent().build();
    }
}
