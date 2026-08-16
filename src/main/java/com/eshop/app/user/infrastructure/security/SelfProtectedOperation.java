package com.eshop.app.user.infrastructure.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Operations that admins are blocked from performing on themselves. */
@RequiredArgsConstructor
@Getter
public enum SelfProtectedOperation {
    DELETE("Cannot delete your own account", "USER_SELF_DELETE"),
    DEACTIVATE("Cannot deactivate your own account", "USER_SELF_DEACTIVATE"),
    ROLE_CHANGE("Cannot change your own role", "USER_SELF_ROLE_CHANGE"),
    ACTIVATE("Cannot activate your own account", "USER_SELF_ACTIVATE");

    private final String message;
    private final String errorCode;
}
