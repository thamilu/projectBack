package com.eshop.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PriceDiscountValidator.class)
@Target({ TYPE })
@Retention(RUNTIME)
public @interface ValidPriceDiscount {
    String message() default "Discount price must be less than regular price";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
