package com.nitros64.nitros_games_backend.constrait;

import com.nitros64.nitros_games_backend.validator.NoNumberValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = NoNumberValidator.class)
@Target({METHOD, FIELD, PARAMETER}) //Zonas Soportadas
@Retention(RUNTIME) //Funciona en el Runtime
public @interface NoNumberString {
    String message() default "No Debe contener Numeros";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
