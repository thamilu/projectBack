package com.eshop.app.user.api.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Constraint(validatedBy = RedirectUriValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRedirectUri {
    String message() default "Invalid redirect URI";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
