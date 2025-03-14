package com.nitros64.nitros_games_backend.exception.apierror;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ValidationError {
    private HttpStatus httpStatus;
    private Map<String, List<ValidationErrorEntry>> errors;
}
