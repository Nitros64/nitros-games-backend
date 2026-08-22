package com.nitros64.nitros_games_backend.storage.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.nitros64.nitros_games_backend.storage.application.UploadImageException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class FileExceptionHandler {
    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e){
        Map<String, Object> response = new HashMap<>();
        response.put("message","El tamaño del archivo es superior a 10M");
        return new ResponseEntity<>(response, HttpStatus.CONTENT_TOO_LARGE);
    }

    @ExceptionHandler(value = UploadImageException.class)
    public ResponseEntity<?> handleUploadImageException(UploadImageException e){
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        body.put("message",e.getMessage());
        body.put("exception","UploadImageException");

        if(e.getCause() != null)
            body.put("error", e.getCause().getMessage());

        response.put("FileError", body);
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }
}
