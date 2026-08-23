package com.nitros64.nitros_games_backend.tooling.api;

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

import com.nitros64.nitros_games_backend.shared.api.PageResponse;
import com.nitros64.nitros_games_backend.shared.api.ApiResponse;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgramToolTypeRequest;
import com.nitros64.nitros_games_backend.tooling.api.dto.ProgramToolTypeResponse;
import com.nitros64.nitros_games_backend.tooling.api.mapper.ProgramToolTypeApiMapper;
import com.nitros64.nitros_games_backend.tooling.application.ProgramToolTypeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping({"/api/v1/programming-tool-types", "/api/v1/programtooltypes"})
public class ProgramToolTypeController {

    private final ProgramToolTypeService service;
    private final ProgramToolTypeApiMapper mapper;

    public ProgramToolTypeController(
            ProgramToolTypeService service,
            ProgramToolTypeApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<ProgramToolTypeResponse>> getAll() {
        return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<ProgramToolTypeResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.findAll(pageable), mapper::toResponse));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProgramToolTypeResponse>> search(
            @RequestParam @NotBlank @Size(max = 30) String name,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                service.searchByName(name, pageable),
                mapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramToolTypeResponse> getOne(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.findById(id)));
    }

    @PostMapping(path = {"", "/add"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProgramToolTypeResponse> save(
            @Valid @RequestBody ProgramToolTypeRequest request) {
        var response = mapper.toResponse(service.save(mapper.toEntity(request)));
        return ApiResponse.created(response, "/api/v1/programming-tool-types/{id}", response.id());
    }

    @PostMapping({"/batch", "/addAll"})
    public ResponseEntity<List<ProgramToolTypeResponse>> saveAll(
            @RequestBody List<@Valid ProgramToolTypeRequest> requests) {
        var entities = requests.stream().map(mapper::toEntity).toList();
        var response = service.saveAll(entities).stream().map(mapper::toResponse).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgramToolTypeResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProgramToolTypeRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.update(id, mapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
