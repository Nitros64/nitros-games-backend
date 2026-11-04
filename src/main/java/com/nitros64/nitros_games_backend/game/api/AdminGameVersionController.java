package com.nitros64.nitros_games_backend.game.api;

import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Positive;
import com.nitros64.nitros_games_backend.game.api.dto.GameVersionResponse;
import com.nitros64.nitros_games_backend.game.api.mapper.GameVersionApiMapper;
import com.nitros64.nitros_games_backend.game.application.GameVersionApplicationService;

/** Administrative reads; the /api/v1/admin/** security rule requires ADMIN. */
@RestController
@Validated
@RequestMapping("/api/v1/admin/games/{gameId}/versions")
public class AdminGameVersionController {
    private final GameVersionApplicationService service;
    private final GameVersionApiMapper mapper;

    public AdminGameVersionController(GameVersionApplicationService service, GameVersionApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<GameVersionResponse> findAll(@PathVariable @Positive Long gameId) {
        return service.findAdminVersions(gameId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{versionId}")
    public GameVersionResponse findOne(@PathVariable @Positive Long gameId, @PathVariable @Positive Long versionId) {
        return mapper.toResponse(service.findAdminVersion(gameId, versionId));
    }
}
