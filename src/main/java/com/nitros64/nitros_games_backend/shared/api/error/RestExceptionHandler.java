package com.nitros64.nitros_games_backend.shared.api.error;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.nitros64.nitros_games_backend.shared.application.ResourceNotFoundException;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Stream<ApiValidationViolation> fieldViolations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::fieldViolation);
        Stream<ApiValidationViolation> globalViolations = ex.getBindingResult()
                .getGlobalErrors()
                .stream()
                .map(this::globalViolation);

        ProblemDetail problem = ApiProblem.validation(
                "One or more request fields are invalid",
                requestPath(request),
                Stream.concat(fieldViolations, globalViolations).toList());
        return response(problem, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ApiValidationViolation> violations = ex.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error ->
                        new ApiValidationViolation(
                                Optional.ofNullable(result.getMethodParameter().getParameterName())
                                        .orElse("argument"),
                                errorCode(error),
                                Optional.ofNullable(error.getDefaultMessage())
                                        .orElse("Invalid value"))))
                .toList();

        return response(ApiProblem.validation(
                "One or more request parameters are invalid",
                requestPath(request),
                violations), headers);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {
        List<ApiValidationViolation> violations = ex.getConstraintViolations()
                .stream()
                .map(this::constraintViolation)
                .toList();
        return response(ApiProblem.validation(
                "One or more request parameters are invalid",
                requestPath(request),
                violations));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return response(ApiProblem.create(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                "malformed_json",
                "The request body is not valid JSON",
                requestPath(request)), headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return response(ApiProblem.create(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type",
                "unsupported_media_type",
                "The request content type is not supported",
                requestPath(request)), headers);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String parameter = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        String detail = parameter == null
                ? "A request value has an invalid type"
                : "Parameter '" + parameter + "' has an invalid type";
        return response(ApiProblem.create(
                HttpStatus.BAD_REQUEST,
                "Invalid parameter",
                "type_mismatch",
                detail,
                requestPath(request)), headers);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return response(ApiProblem.create(
                HttpStatus.BAD_REQUEST,
                "Missing parameter",
                "missing_parameter",
                "Required parameter '" + ex.getParameterName() + "' is missing",
                requestPath(request)), headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return response(ApiProblem.create(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "method_not_allowed",
                "The HTTP method is not supported for this resource",
                requestPath(request)), headers);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return notFound(headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return notFound(headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return response(ApiProblem.create(
                HttpStatus.CONTENT_TOO_LARGE,
                "Upload too large",
                "upload_too_large",
                "The uploaded request exceeds the configured size limit",
                requestPath(request)), headers);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<Object> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request) {
        return response(ApiProblem.create(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "resource_not_found",
                ex.getMessage(),
                requestPath(request)));
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            org.hibernate.exception.ConstraintViolationException.class
    })
    ResponseEntity<Object> handleDataIntegrity(Exception ex, WebRequest request) {
        log.warn("Data integrity conflict at {}", requestPath(request));
        return response(ApiProblem.create(
                HttpStatus.CONFLICT,
                "Data conflict",
                "data_conflict",
                "The request conflicts with existing data",
                requestPath(request)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled request failure at {}", requestPath(request), ex);
        return response(ApiProblem.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "internal_error",
                "The request could not be completed",
                requestPath(request)));
    }

    private ResponseEntity<Object> notFound(HttpHeaders headers, WebRequest request) {
        return response(ApiProblem.create(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                "resource_not_found",
                "The requested resource does not exist",
                requestPath(request)), headers);
    }

    private ResponseEntity<Object> response(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private ResponseEntity<Object> response(ProblemDetail problem, HttpHeaders headers) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return ResponseEntity.status(problem.getStatus())
                .headers(responseHeaders)
                .body(problem);
    }

    private String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return null;
    }

    private ApiValidationViolation fieldViolation(FieldError error) {
        return new ApiValidationViolation(
                error.getField(),
                errorCode(error),
                Optional.ofNullable(error.getDefaultMessage()).orElse("Invalid value"));
    }

    private ApiValidationViolation globalViolation(ObjectError error) {
        return new ApiValidationViolation(
                error.getObjectName(),
                errorCode(error),
                Optional.ofNullable(error.getDefaultMessage()).orElse("Invalid value"));
    }

    private ApiValidationViolation constraintViolation(ConstraintViolation<?> violation) {
        String field = "value";
        for (jakarta.validation.Path.Node node : violation.getPropertyPath()) {
            if (node.getName() != null) {
                field = node.getName();
            }
        }
        return new ApiValidationViolation(
                field,
                violation.getConstraintDescriptor().getAnnotation()
                        .annotationType().getSimpleName(),
                violation.getMessage());
    }

    private String errorCode(org.springframework.context.MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        return codes == null || codes.length == 0 ? "invalid" : codes[codes.length - 1];
    }
}
