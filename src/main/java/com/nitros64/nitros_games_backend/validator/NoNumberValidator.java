package com.nitros64.nitros_games_backend.validator;

import com.nitros64.nitros_games_backend.constrait.NoNumberString;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoNumberValidator implements ConstraintValidator<NoNumberString, String> {

    @Override
    public boolean isValid(String nameField, ConstraintValidatorContext context) {
        boolean boleani = nameField != null && !nameField.matches(".*\\d.*");
        return boleani;
    }
}
/*
    ConstraintValidator<NoNumberString, String>
    NoNumberString es el nombre de la anotacion
    String en este caso es el segundo parametro y debe ser del mismo tipo de dato que se quiere validad
    El parametro name no debe contener numeros asi que se debe comprobar la existencia de numeros

    str.matches(".*\\d.*")
*/