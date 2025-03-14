package com.nitros64.nitros_games_backend.exception.exceptionhandler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.nitros64.nitros_games_backend.exception.apierror.ApiError;
import com.nitros64.nitros_games_backend.exception.apierror.CustomExceptionMessage;
import com.nitros64.nitros_games_backend.exception.apierror.ValidationError;
import com.nitros64.nitros_games_backend.exception.httpexception.NotFoundException;

import java.util.HashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler
{
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,HttpStatusCode status, WebRequest request) {

        return CustomExceptionMessage.handleHttpMediaTypeNotSupportedResponse(ex);
    } 

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                          HttpHeaders headers,HttpStatusCode status, WebRequest request) {

        ValidationError validationError = CustomExceptionMessage.ArgumentNotValidMessage(ex);
        return handleExceptionInternal(ex, validationError, headers, validationError.getHttpStatus(), request);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        return CustomExceptionMessage.customValidationErrorMessage(ex);
    }

    @ExceptionHandler(value = { org.hibernate.exception.ConstraintViolationException.class })
    protected ResponseEntity<Object> handleConstraintViolation(org.hibernate.exception.ConstraintViolationException ex) {
        return CustomExceptionMessage.handleConstraintViolation(ex);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return CustomExceptionMessage.customURLWebPageErrorMessage(ex);
    }
    
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiError apiError = new ApiError(BAD_REQUEST);
        apiError.setMessage(String.format("Could not find the %s method for URL %s", ex.getHttpMethod(), ex.getRequestURL()));
        apiError.setDebugMessage(ex.getMessage());
        return buildResponseEntity(apiError);
    }
    
    /**
     * Handle Exception, handle generic Exception.class
     *
     * @param ex the Exception
     * @return the ApiError object
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                      WebRequest request) {
        String message = String.format("The parameter '%s' of value '%s' could not be converted to type '%s' ", ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());
        Map<String, Object> errors = new HashMap<>();

        errors.put("Message",  message);
        errors.put("Exception","springframework: MethodArgumentTypeMismatchException");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundException(NotFoundException ex){
        Map<String, Object> errors = new HashMap<>();
        errors.put("Message",  ex.getMessage());
        errors.put("Exception","NotFoundException");
        return ResponseEntity.status(ex.getHttpStatus()).body(errors);
    }
    
    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return ResponseEntity.status(apiError.getStatus()).body(apiError);
    }
}