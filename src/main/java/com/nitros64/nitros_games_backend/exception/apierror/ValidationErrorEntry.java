package com.nitros64.nitros_games_backend.exception.apierror;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ValidationErrorEntry {
    private String code;
    private String message;
    private String invalidValue;

    public ValidationErrorEntry(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ValidationErrorEntry(String code, String message, String invalidValue) {
        this.code = code;
        this.message = message;
        this.invalidValue = invalidValue;
    }

}
