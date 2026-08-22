package com.nitros64.nitros_games_backend.tooling.api.dto;

import java.net.URL;

public record ProgrammingToolResponse(
        Long id,
        String name,
        URL webPage,
        String imagefilePath,
        Long toolTypeId) {
}
