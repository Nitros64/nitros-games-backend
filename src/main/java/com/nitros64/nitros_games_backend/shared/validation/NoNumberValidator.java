package com.nitros64.nitros_games_backend.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoNumberValidator implements ConstraintValidator<NoNumberString, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && !value.matches(".*\\d.*");
    }
}
