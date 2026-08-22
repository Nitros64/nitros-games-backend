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

import com.nitros64.nitros_games_backend.catalog.api.dto.PlatformRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.PlatformResponse;
import com.nitros64.nitros_games_backend.catalog.api.mapper.PlatformApiMapper;
import com.nitros64.nitros_games_backend.catalog.application.PlatformService;
import com.nitros64.nitros_games_backend.shared.api.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/v1/platform")
public class PlatformController {

    private final PlatformService service;
    private final PlatformApiMapper mapper;

    public PlatformController(PlatformService service, PlatformApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<PlatformResponse>> getAll() {
        return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<PlatformResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.findAll(pageable), mapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlatformResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.findById(id)));
    }

    @PostMapping(path = "add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlatformResponse> save(@Valid @RequestBody PlatformRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(service.save(mapper.toEntity(request))));
    }

    @PostMapping("addAll")
    public ResponseEntity<List<PlatformResponse>> saveAll(
            @RequestBody List<@Valid PlatformRequest> requests) {
        var entities = requests.stream().map(mapper::toEntity).toList();
        var response = service.saveAll(entities).stream().map(mapper::toResponse).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlatformResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PlatformRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toResponse(service.update(id, mapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
