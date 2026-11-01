package com.nitros64.nitros_games_backend.game.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.game.api.dto.GameResponse;
import com.nitros64.nitros_games_backend.game.api.dto.PublicationRequest;
import com.nitros64.nitros_games_backend.game.api.mapper.GameApiMapper;
import com.nitros64.nitros_games_backend.game.application.GameApplicationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@Validated
@RequestMapping("/api/v1/admin/games")
public class AdminGameController {
    private final GameApplicationService service;
    private final GameApiMapper mapper;

    public AdminGameController(GameApplicationService service, GameApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<GameResponse> findAll() {
        return service.findAdminGames().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public GameResponse findOne(@PathVariable @Positive Long id) {
        return mapper.toResponse(service.findAdminGame(id));
    }

    @PutMapping(path = "/{id}/publication", consumes = MediaType.APPLICATION_JSON_VALUE)
    public GameResponse changePublication(@PathVariable @Positive Long id,
            @Valid @RequestBody PublicationRequest request) {
        return mapper.toResponse(service.changePublicationStatus(id, request.publicationStatus()));
    }
}
