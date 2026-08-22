package com.nitros64.nitros_games_backend.storage.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nitros64.nitros_games_backend.shared.api.error.ApiProblem;
import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class FileExceptionHandler {

    @ExceptionHandler(value = UploadImageException.class)
    public ResponseEntity<ProblemDetail> handleUploadImageException(
            UploadImageException exception,
            HttpServletRequest request) {
        ProblemDetail problem = ApiProblem.create(
                exception.getHttpStatus(),
                "Image storage error",
                "image_storage_error",
                exception.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(exception.getHttpStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
