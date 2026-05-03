package com.eshop.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class SafeTextValidator implements ConstraintValidator<SafeText, String> {

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE|EXEC|EXECUTE)\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(<script|javascript:|on\\w+\\s*=|eval\\s*\\(|expression\\s*\\()", Pattern.CASE_INSENSITIVE);
    private static final Pattern DANGEROUS_CHARS_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        if (SQL_INJECTION_PATTERN.matcher(value).find()) {
            setMessage(context, "Text contains potentially unsafe SQL keywords");
            return false;
        }

        if (SCRIPT_PATTERN.matcher(value).find()) {
            setMessage(context, "Text contains potentially unsafe script content");
            return false;
        }

        if (DANGEROUS_CHARS_PATTERN.matcher(value).find()) {
            setMessage(context, "Text contains invalid control characters");
            return false;
        }

        return true;
    }

    private void setMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
