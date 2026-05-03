package com.eshop.app.validation;

import com.eshop.app.dto.request.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegisterRequest> {

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        // no initialization required
    }

    @Override
    public boolean isValid(RegisterRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // let @NotNull/@NotBlank handle null checks on fields
        }

        String pwd = value.getPassword();
        String confirm = value.getConfirmPassword();

        if (pwd == null || confirm == null) {
            return false;
        }

        return pwd.equals(confirm);
    }
}
