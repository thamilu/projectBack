package com.eshop.app.validation;

import com.eshop.app.dto.request.ProductCreateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PriceDiscountValidator
        implements ConstraintValidator<ValidPriceDiscount, ProductCreateRequest> {

    @Override
    public boolean isValid(ProductCreateRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        if (request.getDiscountPrice() == null) return true; // nothing to validate

        if (request.getPrice() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Regular price is required when discount price is set"
            ).addPropertyNode("price").addConstraintViolation();
            return false;
        }

        if (request.getDiscountPrice().compareTo(request.getPrice()) >= 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Discount price must be less than regular price"
            ).addPropertyNode("discountPrice").addConstraintViolation();
            return false;
        }

        return true;
    }
}
