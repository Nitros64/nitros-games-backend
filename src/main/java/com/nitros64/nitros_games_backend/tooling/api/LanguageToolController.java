package com.nitros64.nitros_games_backend.tooling.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.shared.api.ApiResponse;
import com.nitros64.nitros_games_backend.tooling.api.dto.LanguageToolRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.LanguageToolResponse;
import com.nitros64.nitros_games_backend.tooling.api.mapper.LanguageToolApiMapper;
import com.nitros64.nitros_games_backend.tooling.application.LanguageToolService;

@RestController
@Validated
@RequestMapping("/api/v1/language-tools")
public class LanguageToolController {

    private final LanguageToolService service;
    private final LanguageToolApiMapper mapper;

    public LanguageToolController(LanguageToolService service, LanguageToolApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<LanguageToolResponse>> getAll() {
        return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LanguageToolResponse> create(@Valid @RequestBody LanguageToolRequest request) {
        var response = mapper.toResponse(service.create(
                request.programmingLanguageId(), request.programmingToolId()));
        return ApiResponse.created(response, "/api/v1/language-tools/{languageId}/{toolId}",
                response.programmingLanguageId(), response.programmingToolId());
    }

    @DeleteMapping("/{languageId}/{toolId}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long languageId, @PathVariable @Positive Long toolId) {
        service.delete(languageId, toolId);
        return ResponseEntity.noContent().build();
    }
}
