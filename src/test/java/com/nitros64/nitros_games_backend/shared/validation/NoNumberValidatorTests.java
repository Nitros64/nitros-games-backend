package com.nitros64.nitros_games_backend.shared.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoNumberValidatorTests {

    private final NoNumberValidator validator = new NoNumberValidator();

    @Test
    void acceptsTextWithoutNumbers() {
        assertTrue(validator.isValid("MediaFire", null));
    }

    @Test
    void rejectsTextContainingNumbers() {
        assertFalse(validator.isValid("Mega2", null));
    }

    @Test
    void rejectsNull() {
        assertFalse(validator.isValid(null, null));
    }
}
