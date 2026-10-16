package edu.uow.csit314.renovation.registration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegistrationPasswordValidatorTest {
    private final RegistrationPasswordValidator validator = new RegistrationPasswordValidator();

    @Test
    void acceptsCompliantPasswordWithSpacesAndUnicode() {
        assertTrue(validator.isValid("Hello Pass1!", null));
        assertTrue(validator.isValid("ÉclairTest1!", null));
    }

    @Test
    void rejectsNullShortLongAndMissingCharacterTypes() {
        assertFalse(validator.isValid(null, null));
        assertFalse(validator.isValid("Aa1!short", null));
        assertFalse(validator.isValid("A".repeat(125) + "a1!x", null));
        assertFalse(validator.isValid("alllowercase1!", null));
        assertFalse(validator.isValid("ALLUPPERCASE1!", null));
        assertFalse(validator.isValid("HelloWorld!", null));
        assertFalse(validator.isValid("HelloWorld1", null));
    }
}
