package com.nitros64.nitros_games_backend.shared.api.error;

public record ApiValidationViolation(String field, String code, String message) {
}
