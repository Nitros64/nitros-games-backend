package com.nitros64.nitros_games_backend.storage.application;

import org.springframework.http.HttpStatus;

public class UploadImageException extends RuntimeException{
    private HttpStatus httpStatus;
    public UploadImageException(String message) {
        super(message);
    }

    public UploadImageException(String message, Throwable cause) {
        super(message, cause);
    }

    public UploadImageException(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public UploadImageException(String message, Throwable cause,HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
