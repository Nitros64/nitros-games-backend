package com.nitros64.nitros_games_backend.catalog.api;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.catalog.api.dto.GameGenreRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.GameGenreResponse;
import com.nitros64.nitros_games_backend.catalog.api.mapper.GameGenreApiMapper;
import com.nitros64.nitros_games_backend.catalog.application.GameGenreService;
import com.nitros64.nitros_games_backend.shared.api.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/v1/gamegenre")
public class GameGenreController {

    private final GameGenreService service;
    private final GameGenreApiMapper mapper;

    public GameGenreController(GameGenreService service, GameGenreApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<GameGenreResponse>> getAll() {
        return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<GameGenreResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.findAll(pageable), mapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameGenreResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.findById(id)));
    }

    @PostMapping(path = "add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GameGenreResponse> save(@Valid @RequestBody GameGenreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(service.save(mapper.toEntity(request))));
    }

    @PostMapping("addAll")
    public ResponseEntity<List<GameGenreResponse>> saveAll(
            @RequestBody List<@Valid GameGenreRequest> requests) {
        var entities = requests.stream().map(mapper::toEntity).toList();
        var response = service.saveAll(entities).stream().map(mapper::toResponse).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameGenreResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody GameGenreRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toResponse(service.update(id, mapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
