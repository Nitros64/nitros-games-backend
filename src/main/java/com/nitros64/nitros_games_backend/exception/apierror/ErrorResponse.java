package com.nitros64.nitros_games_backend.exception.apierror;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ErrorResponse {
    private final List<String> errors;
    private final Map<String, Object> response = new HashMap<>();     
    private final HttpStatus status;
    
    public ErrorResponse(List<String> errors, HttpStatus status,Exception ex){
        this.errors = errors;
        this.status = status;        
        response.put("errors", errors);
        //response.put("production_debug_message", ex.getMessage());
    }
    
    private ResponseEntity<Object> errorUserResponse(){
        return ResponseEntity.status(status).body(response);
        //return new ResponseEntity(response, status);
    }
    
    private ResponseEntity<?> errorDeveloperResponse(){
        return null;
    }
    
    public ResponseEntity<Object> errorResponse(){
        return this.errorUserResponse();
    }
    
}
