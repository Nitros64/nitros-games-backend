package com.nitros64.nitros_games_backend.shared.api.error;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ApiProblem {

    private ApiProblem() {
    }

    public static ProblemDetail create(
            HttpStatus status,
            String title,
            String code,
            String detail,
            String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("code", code);
        if (instance != null && !instance.isBlank()) {
            problem.setInstance(URI.create(instance));
        }
        return problem;
    }

    public static ProblemDetail validation(
            String detail,
            String instance,
            List<ApiValidationViolation> violations) {
        ProblemDetail problem = create(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "validation_failed",
                detail,
                instance);
        problem.setProperty("errors", violations);
        return problem;
    }
}
