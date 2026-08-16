package com.eshop.app.user.api.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("requestRedirectUriValidator")
@RequiredArgsConstructor
public class RedirectUriValidator implements ConstraintValidator<ValidRedirectUri, String> {

    private final com.eshop.app.core.infrastructure.config.security.oauth.RedirectUriValidator
            coreValidator;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Let @NotBlank handle blank values if needed
        }
        return coreValidator.isAllowed(value);
    }
}
