package com.nitros64.nitros_games_backend.game.api;

import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Positive;
import com.nitros64.nitros_games_backend.game.api.dto.DownloadLinkResponse;
import com.nitros64.nitros_games_backend.game.api.mapper.DownloadLinkApiMapper;
import com.nitros64.nitros_games_backend.game.application.DownloadLinkApplicationService;

/** Administrative reads; the /api/v1/admin/** security rule requires ADMIN. */
@RestController
@Validated
@RequestMapping("/api/v1/admin/games/{gameId}/versions/{versionId}/download-links")
public class AdminDownloadLinkController {
    private final DownloadLinkApplicationService service;
    private final DownloadLinkApiMapper mapper;

    public AdminDownloadLinkController(DownloadLinkApplicationService service, DownloadLinkApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public List<DownloadLinkResponse> findAll(@PathVariable @Positive Long gameId, @PathVariable @Positive Long versionId) {
        return service.findAdminDownloadLinks(gameId, versionId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{linkId}")
    public DownloadLinkResponse findOne(@PathVariable @Positive Long gameId, @PathVariable @Positive Long versionId, @PathVariable @Positive Long linkId) {
        return mapper.toResponse(service.findAdminDownloadLink(gameId, versionId, linkId));
    }
}
