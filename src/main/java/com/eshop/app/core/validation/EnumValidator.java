package com.eshop.app.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    private Class<? extends Enum<?>> enumClass;
    private Set<String> acceptedValues;

    @Override
    public void initialize(ValidEnum annotation) {
        this.enumClass = annotation.enumClass();
        this.acceptedValues =
                Arrays.stream(enumClass.getEnumConstants())
                        .filter(Objects::nonNull)
                        .map(e -> e.name())
                        .map(name -> name.toUpperCase())
                        .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Use @NotNull or @NotEmpty if null values are not allowed
        }

        String trimmedValue = value.trim();

        // 1. Try case-insensitive enum name match
        if (acceptedValues.contains(trimmedValue.toUpperCase())) {
            return true;
        }

        // 2. Try fromString method (e.g. returning Optional<Enum>)
        try {
            java.lang.reflect.Method fromStringMethod =
                    enumClass.getMethod("fromString", String.class);
            Object result = fromStringMethod.invoke(null, trimmedValue);
            if (result instanceof java.util.Optional<?> opt) {
                return opt.isPresent();
            } else if (result != null) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // 3. Fallback: check toString(), getDisplayName(), and getCode()
        // case-insensitively
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.toString().equalsIgnoreCase(trimmedValue)) {
                return true;
            }
            try {
                java.lang.reflect.Method getDisplayName =
                        enumConstant.getClass().getMethod("getDisplayName");
                String displayName = (String) getDisplayName.invoke(enumConstant);
                if (displayName != null && displayName.equalsIgnoreCase(trimmedValue)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
            try {
                java.lang.reflect.Method getCode = enumConstant.getClass().getMethod("getCode");
                String code = (String) getCode.invoke(enumConstant);
                if (code != null && code.equalsIgnoreCase(trimmedValue)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }
}
