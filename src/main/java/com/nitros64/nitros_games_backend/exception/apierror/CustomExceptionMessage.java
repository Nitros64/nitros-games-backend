package com.nitros64.nitros_games_backend.exception.apierror;

import java.util.*;
import java.util.stream.Collectors;

import org.hibernate.validator.internal.engine.path.NodeImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.hibernate.validator.internal.metadata.descriptor.ConstraintDescriptorImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;

 /**************************************************************
 *                      EXCEPTION MESSAGE                      *
 **************************************************************/

public class CustomExceptionMessage {
    public static ResponseEntity<Object>  customURLWebPageErrorMessage(org.springframework.http.converter.HttpMessageNotReadableException ex){
        Map<String, Object> errors = new HashMap<>();

        errors.put("Message",  "JSON Error, MalFormed");
        errors.put("Exception","springframework: HttpMessageNotReadableException");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    public static List<String> customValidationErrorMessage(MethodArgumentNotValidException ex){
        List<String> errors = ex.getBindingResult().getFieldErrors()
                        .stream()
                        .map(err -> err.getField() + ": " + err.getDefaultMessage())
                        .collect(Collectors.toList());
        return errors;
    }

    /*
     * Construccion del Mensaje para la excepcion handleMethodArgumentNotValid
     *
     */
    public static ValidationError ArgumentNotValidMessage(MethodArgumentNotValidException ex){
        Map<String, List<ValidationErrorEntry>> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {

            String field = error.getField(); //Extrae el nombre de atributo de la clase que esta generando problemas

            if (!errors.containsKey(field)) {
                errors.put(field, new ArrayList()); //agrega e campo como etiqueta y agrega un arreglo para anotar los errores provocados por el atributo
            }
            errors.get(field).add(new ValidationErrorEntry(error.getCode(), error.getDefaultMessage(), (String) error.getRejectedValue()));
        }

        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            String objectName = error.getObjectName();
            if (!errors.containsKey(objectName)) {
                errors.put(objectName, new ArrayList());
            }
            errors.get(objectName).add(new ValidationErrorEntry(error.getCode(), error.getDefaultMessage()));
        }

        return new ValidationError(HttpStatus.BAD_REQUEST, errors); //Mensaje de error
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static ResponseEntity<Object> customValidationErrorMessage(jakarta.validation.ConstraintViolationException ex){
        Map<String, List<ValidationErrorEntry>> errors = new HashMap<>();
        Set<ConstraintViolation<?>> set =  ex.getConstraintViolations();

        for (ConstraintViolation<?> next : set) {
            NodeImpl nodeI = ((PathImpl) next.getPropertyPath()).getLeafNode();
            String field = nodeI.getName(); //Extrae el nombre de atributo de la clase que esta generando problemas
            String code = ( (ConstraintDescriptorImpl) next.getConstraintDescriptor() ).getAnnotationDescriptor().getType().getSimpleName();

            if (!errors.containsKey(field)) {
                errors.put(field, new ArrayList());//agrega e campo como etiqueta y agrega un arreglo para anotar los errores provocados por el atributo
            }
            errors.get(field).add(new ValidationErrorEntry(code, next.getMessage(), (String) next.getInvalidValue()));
        }
        return ResponseEntity.badRequest()
                .body(new ValidationError(HttpStatus.BAD_REQUEST, errors));
    }

    public static ResponseEntity<Object> handleConstraintViolation(org.hibernate.exception.ConstraintViolationException ex){

        Map<String, Object> errors = new HashMap<>();
        errors.put("Message General",  ex.getMessage());
        errors.put("Message SQL",ex.getSQLException().getMessage());
        errors.put("Exception","hibernate ConstraintViolationException");
        return ResponseEntity
                .status(HttpStatus.NOT_ACCEPTABLE)
                .body(errors);
    }
     public static ResponseEntity<Object> handleHttpMediaTypeNotSupportedResponse(org.springframework.web.HttpMediaTypeNotSupportedException ex){
         Map<String, Object> errors = new HashMap<>();
         errors.put("Message",  ex.getMessage());
         errors.put("Exception","springframework: HttpMediaTypeNotSupportedException");
         return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errors);
     }
}