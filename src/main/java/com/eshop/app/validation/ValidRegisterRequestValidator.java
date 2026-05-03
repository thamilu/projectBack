package com.eshop.app.validation;

import com.eshop.app.dto.request.RegisterRequest;
import com.eshop.app.entity.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidRegisterRequestValidator implements ConstraintValidator<ValidRegisterRequest, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        // Don't disable default constraint violations - let field-level validations work
        // Only add custom validations for role-specific logic
        
        // Determine effective role
        Role effectiveRole = getEffectiveRole(request);
        
        // Only perform role-specific validation, don't interfere with basic field validation
        boolean valid = true;

        // Role-specific checks (only if we have a valid role)
        if (effectiveRole == Role.DELIVERY_AGENT) {
            if (isBlank(request.getVehicleType())) {
                context.buildConstraintViolationWithTemplate("vehicleType is required for delivery agents")
                        .addPropertyNode("vehicleType").addConstraintViolation();
                valid = false;
            }
        } else if (effectiveRole == Role.SELLER) {
            String sellerType = request.getSellerType();
            if (isBlank(sellerType)) {
                context.buildConstraintViolationWithTemplate("sellerType is required when role is SELLER")
                        .addPropertyNode("sellerType").addConstraintViolation();
                valid = false;
            } else {
                String st = sellerType.trim().toUpperCase();
                // All sellers require storeName and businessName
                if (isBlank(request.getStoreName())) {
                    context.buildConstraintViolationWithTemplate("storeName is required for this seller type")
                            .addPropertyNode("storeName").addConstraintViolation();
                    valid = false;
                }
                if (isBlank(request.getBusinessName())) {
                    context.buildConstraintViolationWithTemplate("businessName is required for this seller type")
                            .addPropertyNode("businessName").addConstraintViolation();
                    valid = false;
                }
            }
        }

        return valid;
    }

    private Role getEffectiveRole(RegisterRequest request) {
        Role role = request.getRole();
        String roleName = request.getRoleName();
        
        if (role != null) {
            return role;
        } else if (roleName != null && !roleName.isBlank()) {
            try {
                return Role.valueOf(roleName.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // tolerate some common aliases
                String rn = roleName.trim().toUpperCase();
                if (rn.equals("DELIVERY")) return Role.DELIVERY_AGENT;
                else if (rn.equals("SELLER")) return Role.SELLER;
            }
        }
        
        return null; // Let field-level validation or business logic handle missing role
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
