package com.eshop.app.user.application.service;

import com.eshop.app.core.infrastructure.config.security.keycloak.KeycloakConfig;
import com.eshop.app.user.domain.exception.KeycloakErrorType;
import com.eshop.app.user.domain.exception.KeycloakOperationException;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validator component for checking inputs to Keycloak operations. Protects boundaries by throwing
 * KeycloakOperationException for invalid requests.
 */
@Component
public class KeycloakServiceValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final int MAX_EMAIL_LENGTH = 254;

    private final KeycloakConfig keycloakConfig;
    private final MaskingUtil maskingUtil;

    public KeycloakServiceValidator(KeycloakConfig keycloakConfig, MaskingUtil maskingUtil) {
        this.keycloakConfig = Objects.requireNonNull(keycloakConfig, "keycloakConfig");
        this.maskingUtil = Objects.requireNonNull(maskingUtil, "maskingUtil");
    }

    public void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new KeycloakOperationException(
                    KeycloakErrorType.INVALID_REQUEST, "userId must not be blank", null);
        }
        if (!isValidUuid(userId)) {
            throw new KeycloakOperationException(
                    KeycloakErrorType.INVALID_REQUEST,
                    "Invalid userId format — expected UUID, got: " + maskingUtil.maskUserId(userId),
                    null);
        }
    }

    public boolean isValidUuid(String id) {
        if (!StringUtils.hasText(id)) return false;
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new KeycloakOperationException(
                    KeycloakErrorType.INVALID_REQUEST, "email must not be blank", null);
        }
        if (email.length() > MAX_EMAIL_LENGTH) {
            throw new KeycloakOperationException(
                    KeycloakErrorType.INVALID_REQUEST,
                    "Email exceeds max length of 254 chars",
                    null);
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new KeycloakOperationException(
                    KeycloakErrorType.INVALID_REQUEST, "Invalid email format", null);
        }
    }

    public void validateRoleName(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            throw new KeycloakOperationException(
                    KeycloakErrorType.INVALID_REQUEST, "roleName must not be blank", null);
        }
        if (!keycloakConfig.getAllowedRoles().contains(roleName)) {
            throw new KeycloakOperationException(
                    KeycloakErrorType.INVALID_REQUEST,
                    "Role [" + roleName + "] is not in the permitted role set",
                    null);
        }
    }
}
