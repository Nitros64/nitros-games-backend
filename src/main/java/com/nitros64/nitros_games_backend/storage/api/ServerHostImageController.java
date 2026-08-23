package com.nitros64.nitros_games_backend.storage.api;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nitros64.nitros_games_backend.shared.api.PageResponse;
import com.nitros64.nitros_games_backend.shared.api.ApiResponse;
import com.nitros64.nitros_games_backend.shared.api.error.ApiProblem;
import com.nitros64.nitros_games_backend.storage.api.dto.ServerHostImageNameRequest;
import com.nitros64.nitros_games_backend.storage.api.dto.ServerHostImageResponse;
import com.nitros64.nitros_games_backend.storage.api.dto.ServerHostImageUploadRequest;
import com.nitros64.nitros_games_backend.storage.api.mapper.ServerHostImageApiMapper;
import com.nitros64.nitros_games_backend.storage.application.ServerHostImageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("api/v1/serverhostimage")
public class ServerHostImageController {

    private final ServerHostImageService service;
    private final ServerHostImageApiMapper mapper;

    public ServerHostImageController(
            ServerHostImageService service,
            ServerHostImageApiMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<ServerHostImageResponse>> getAll() {
        return ResponseEntity.ok(service.findAll().stream().map(mapper::toResponse).toList());
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<ServerHostImageResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.findAll(pageable), mapper::toResponse));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<ServerHostImageResponse>> search(
            @RequestParam @NotBlank @Size(max = 30) String name,
            Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                service.searchByName(name, pageable),
                mapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerHostImageResponse> getOne(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(mapper.toResponse(service.findById(id)));
    }

    @PostMapping("add")
    public ResponseEntity<ProblemDetail> saveWithoutFile() {
        ProblemDetail problem = ApiProblem.create(
                HttpStatus.FORBIDDEN,
                "Operation not allowed",
                "operation_not_allowed",
                "Host images must be created through the upload endpoint",
                "/api/v1/serverhostimage/add");
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @PostMapping(path = "upload_image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServerHostImageResponse> upload(
            @Valid @ModelAttribute ServerHostImageUploadRequest request) {
        var response = mapper.toResponse(service.create(request.name(), request.fileHostImage()));
        return ApiResponse.created(response, "/api/v1/serverhostimage/{id}", response.id());
    }

    @PutMapping(path = "upload_image/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServerHostImageResponse> updateImage(
            @PathVariable @Positive Long id,
            @Valid @ModelAttribute ServerHostImageUploadRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.updateImage(
                id,
                request.name(),
                request.fileHostImage())));
    }

    @PutMapping(path = "update_name/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServerHostImageResponse> updateName(
            @PathVariable @Positive Long id,
            @Valid @ModelAttribute ServerHostImageNameRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.updateName(id, request.name())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.deleteWithFile(id);
        return ResponseEntity.noContent().build();
    }
}
