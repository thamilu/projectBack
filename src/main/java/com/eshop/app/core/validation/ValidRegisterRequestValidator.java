package com.eshop.app.core.validation;

import com.eshop.app.user.api.request.RegisterRequest;
import com.eshop.app.user.shared.domain.enums.UserRole;




import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidRegisterRequestValidator implements ConstraintValidator<ValidRegisterRequest, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        // Don't disable default constraint violations - let field-level validations work
        // Only add custom validations for role-specific logic
        
        // Determine effective role
        UserRole effectiveRole = getEffectiveRole(request);

        // Only perform role-specific validation, don't interfere with basic field validation
        boolean valid = true;

        // Role-specific checks (only if we have a valid role)
        if (effectiveRole == UserRole.DELIVERY_AGENT) {
            if (isBlank(request.getVehicleType())) {
                context.buildConstraintViolationWithTemplate("vehicleType is required for delivery agents")
                        .addPropertyNode("vehicleType").addConstraintViolation();
                valid = false;
            }
        } else if (effectiveRole == UserRole.SELLER) {
            String sellerType = request.getSellerType();
            if (isBlank(sellerType)) {
                context.buildConstraintViolationWithTemplate("sellerType is required when role is SELLER")
                        .addPropertyNode("sellerType").addConstraintViolation();
                valid = false;
            } else {

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

    private UserRole getEffectiveRole(RegisterRequest request) {
        UserRole role = request.getRole();
        String roleName = request.getRoleName();

        if (role != null) {
            return role;
        } else if (roleName != null && !roleName.isBlank()) {
            try {
                return UserRole.valueOf(roleName.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // tolerate some common aliases
                String rn = roleName.trim().toUpperCase();
                if (rn.equals("DELIVERY")) return UserRole.DELIVERY_AGENT;
                else if (rn.equals("SELLER")) return UserRole.SELLER;
            }
        }
        
        return null; // Let field-level validation or business logic handle missing role
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
