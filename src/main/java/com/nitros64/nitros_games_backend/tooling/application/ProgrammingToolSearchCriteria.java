package com.nitros64.nitros_games_backend.tooling.application;

public record ProgrammingToolSearchCriteria(
        String name,
        Long toolTypeId,
        Long languageId,
        Long platformId,
        Long processorId) {

    public ProgrammingToolSearchCriteria {
        name = name == null || name.isBlank() ? null : name.strip();
    }
}
