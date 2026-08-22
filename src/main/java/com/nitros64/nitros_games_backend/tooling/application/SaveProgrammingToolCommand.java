package com.nitros64.nitros_games_backend.tooling.application;

import java.net.URL;

public record SaveProgrammingToolCommand(
        String name,
        URL webPage,
        String imagefilePath,
        Long toolTypeId) {
}
