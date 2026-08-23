package com.nitros64.nitros_games_backend.shared.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class ApiResponse {

    private ApiResponse() {
    }

    public static <T> ResponseEntity<T> created(
            T body,
            String pathTemplate,
            Object... pathVariables) {
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(pathTemplate)
                .buildAndExpand(pathVariables)
                .toUri();
        return ResponseEntity.created(location).body(body);
    }
}
