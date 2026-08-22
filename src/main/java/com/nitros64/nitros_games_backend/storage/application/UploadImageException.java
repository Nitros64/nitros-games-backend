package com.nitros64.nitros_games_backend.storage.application;

import java.io.Serial;

import org.springframework.http.HttpStatus;

public class UploadImageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus httpStatus;

    public UploadImageException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
