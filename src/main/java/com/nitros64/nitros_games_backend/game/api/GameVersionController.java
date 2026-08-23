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

import com.nitros64.nitros_games_backend.game.api.dto.GameVersionRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionResponse;
import com.nitros64.nitros_games_backend.game.api.mapper.GameApiMapper;
import com.nitros64.nitros_games_backend.game.application.GameApplicationService;
import com.nitros64.nitros_games_backend.shared.api.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@Validated
@RequestMapping("/api/v1/games/{gameId}/versions")
public class GameVersionController {

    private final GameApplicationService service;
    private final GameApiMapper mapper;

    public GameVersionController(GameApplicationService service, GameApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<GameVersionResponse>> findAll(
            @PathVariable @Positive Long gameId) {
        return ResponseEntity.ok(service.findVersions(gameId).stream()
                .map(mapper::toResponse)
                .toList());
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<GameVersionResponse> findOne(
            @PathVariable @Positive Long gameId,
            @PathVariable @Positive Long versionId) {
        return ResponseEntity.ok(mapper.toResponse(service.findVersion(gameId, versionId)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameVersionResponse> create(
            @PathVariable @Positive Long gameId,
            @Valid @RequestBody GameVersionRequest request) {
        var response = mapper.toResponse(service.createVersion(gameId, mapper.toCommand(request)));
        return ApiResponse.created(
                response,
                "/api/v1/games/{gameId}/versions/{versionId}",
                gameId,
                response.id());
    }

    @PutMapping(path = "/{versionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameVersionResponse> update(
            @PathVariable @Positive Long gameId,
            @PathVariable @Positive Long versionId,
            @Valid @RequestBody GameVersionRequest request) {
        return ResponseEntity.ok(mapper.toResponse(
                service.updateVersion(gameId, versionId, mapper.toCommand(request))));
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long gameId,
            @PathVariable @Positive Long versionId) {
        service.deleteVersion(gameId, versionId);
        return ResponseEntity.noContent().build();
    }
}
