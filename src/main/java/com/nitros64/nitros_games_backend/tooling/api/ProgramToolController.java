package com.nitros64.nitros_games_backend.tooling.api;

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

import com.nitros64.nitros_games_backend.shared.api.PageResponse;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingToolRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgrammingToolResponse;
import com.nitros64.nitros_games_backend.tooling.api.mapper.ProgrammingToolApiMapper;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/v1/programmingtools")
public class ProgramToolController {

    private final ProgramToolService service;
    private final ProgrammingToolApiMapper mapper;

    public ProgramToolController(
            ProgramToolService service,
            ProgrammingToolApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<ProgrammingToolResponse>> getAll() {
        return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<ProgrammingToolResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.findAll(pageable), mapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgrammingToolResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.findById(id)));
    }

    @PostMapping(path = "add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProgrammingToolResponse> save(
            @Valid @RequestBody ProgrammingToolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(service.create(mapper.toCommand(request))));
    }

    @PostMapping("addAll")
    public ResponseEntity<List<ProgrammingToolResponse>> saveAll(
            @RequestBody List<@Valid ProgrammingToolRequest> requests) {
        var commands = requests.stream().map(mapper::toCommand).toList();
        var response = service.createAll(commands).stream().map(mapper::toResponse).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgrammingToolResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProgrammingToolRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toResponse(service.updateFromCommand(id, mapper.toCommand(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
