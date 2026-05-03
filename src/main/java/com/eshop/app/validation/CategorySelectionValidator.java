package com.eshop.app.validation;

import com.eshop.app.dto.request.ProductCreateWithCategoryRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CategorySelectionValidator implements ConstraintValidator<ValidCategorySelection, ProductCreateWithCategoryRequest> {

    @Override
    public boolean isValid(ProductCreateWithCategoryRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        boolean hasExistingCategory = request.getCategoryId() != null;
        boolean hasNewCategory = request.getNewCategoryName() != null && !request.getNewCategoryName().isBlank();

        boolean isValid = hasExistingCategory ^ hasNewCategory; // exclusive OR

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            if (hasExistingCategory && hasNewCategory) {
                context.buildConstraintViolationWithTemplate("Cannot specify both categoryId and newCategoryName")
                        .addConstraintViolation();
            } else {
                context.buildConstraintViolationWithTemplate("Either categoryId or newCategoryName is required")
                        .addConstraintViolation();
            }
        }

        return isValid;
    }
}
