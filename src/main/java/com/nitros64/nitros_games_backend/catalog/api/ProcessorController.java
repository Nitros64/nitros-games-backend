package com.nitros64.nitros_games_backend.catalog.api;

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

import com.nitros64.nitros_games_backend.catalog.api.dto.ProcessorRequest;
import com.nitros64.nitros_games_backend.catalog.api.dto.ProcessorResponse;
import com.nitros64.nitros_games_backend.catalog.api.mapper.ProcessorApiMapper;
import com.nitros64.nitros_games_backend.catalog.application.ProcessorService;
import com.nitros64.nitros_games_backend.shared.api.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("api/v1/processor")
public class ProcessorController {

    private final ProcessorService service;
    private final ProcessorApiMapper mapper;

    public ProcessorController(ProcessorService service, ProcessorApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<ProcessorResponse>> getAll() {
        return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<ProcessorResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.findAll(pageable), mapper::toResponse));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProcessorResponse>> search(
            @RequestParam @NotBlank @Size(max = 10) String name,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                service.searchByName(name, pageable),
                mapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessorResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.findById(id)));
    }

    @PostMapping(path = "add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessorResponse> save(@Valid @RequestBody ProcessorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(service.save(mapper.toEntity(request))));
    }

    @PostMapping("addAll")
    public ResponseEntity<List<ProcessorResponse>> saveAll(
            @RequestBody List<@Valid ProcessorRequest> requests) {
        var entities = requests.stream().map(mapper::toEntity).toList();
        var response = service.saveAll(entities).stream().map(mapper::toResponse).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcessorResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProcessorRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toResponse(service.update(id, mapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
