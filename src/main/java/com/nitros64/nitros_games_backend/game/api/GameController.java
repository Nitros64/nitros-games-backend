package com.nitros64.nitros_games_backend.game.api;

import java.util.List;

import org.springframework.data.domain.Pageable;
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

import com.nitros64.nitros_games_backend.game.api.dto.GameRequest;
import com.nitros64.nitros_games_backend.game.api.dto.GameResponse;
import com.nitros64.nitros_games_backend.game.api.mapper.GameApiMapper;
import com.nitros64.nitros_games_backend.game.application.GameApplicationService;
import com.nitros64.nitros_games_backend.game.application.GameSearchCriteria;
import com.nitros64.nitros_games_backend.shared.api.ApiResponse;
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
    public ResponseEntity<GameResponse> findOne(@PathVariable @Positive Long gameId) {
        return ResponseEntity.ok(mapper.toResponse(service.findGame(gameId)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameResponse> create(@Valid @RequestBody GameRequest request) {
        var response = mapper.toResponse(service.createGame(mapper.toCommand(request)));
        return ApiResponse.created(response, "/api/v1/games/{gameId}", response.id());
    }

    @PutMapping(path = "/{gameId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameResponse> update(
            @PathVariable @Positive Long gameId,
            @Valid @RequestBody GameRequest request) {
        return ResponseEntity.ok(
                mapper.toResponse(service.updateGame(gameId, mapper.toCommand(request))));
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long gameId) {
        service.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }
}
