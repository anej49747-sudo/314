package edu.uow.csit314.renovation.registration;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RegistrationPasswordValidator implements ConstraintValidator<ValidRegistrationPassword, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        int length = value.codePointCount(0, value.length());
        if (length < 10 || length > 128) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (int offset = 0; offset < value.length();) {
            int ch = value.codePointAt(offset);
            upper |= ch >= 'A' && ch <= 'Z';
            lower |= ch >= 'a' && ch <= 'z';
            digit |= ch >= '0' && ch <= '9';
            int type = Character.getType(ch);
            special |= type == Character.CONNECTOR_PUNCTUATION
                    || type == Character.DASH_PUNCTUATION
                    || type == Character.START_PUNCTUATION
                    || type == Character.END_PUNCTUATION
                    || type == Character.OTHER_PUNCTUATION
                    || type == Character.MATH_SYMBOL
                    || type == Character.CURRENCY_SYMBOL
                    || type == Character.MODIFIER_SYMBOL
                    || type == Character.OTHER_SYMBOL
                    || type == Character.INITIAL_QUOTE_PUNCTUATION
                    || type == Character.FINAL_QUOTE_PUNCTUATION;
            offset += Character.charCount(ch);
        }
        return upper && lower && digit && special;
    }
}
