package com.eshop.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AtLeastOneNotNullValidator implements ConstraintValidator<AtLeastOneNotNull, Object> {

    private Set<String> excludeFields;

    @Override
    public void initialize(AtLeastOneNotNull annotation) {
        this.excludeFields = Arrays.stream(annotation.excludeFields()).collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object == null) return false;

        try {
            Class<?> clazz = object.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (excludeFields.contains(field.getName()) || field.isSynthetic()) continue;
                    field.setAccessible(true);
                    Object value = field.get(object);
                    if (value != null) {
                        if (value instanceof String) {
                            if (!((String) value).isBlank()) return true;
                        } else {
                            return true;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return false;
        } catch (IllegalAccessException e) {
            return false;
        }
    }
}
