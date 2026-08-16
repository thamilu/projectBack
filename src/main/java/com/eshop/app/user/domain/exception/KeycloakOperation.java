package com.eshop.app.user.domain.exception;

public enum KeycloakOperation {
    ASSIGN_ROLE("assignRole"),
    REVOKE_ROLE("revokeRole"),
    ASSIGN_ROLE_BY_EMAIL("assignRoleByEmail"),
    SET_USER_ENABLED("setUserEnabled"),
    IS_USER_ENABLED("isUserEnabled");

    private final String operationName;

    KeycloakOperation(String operationName) {
        this.operationName = operationName;
    }

    public String getOperationName() {
        return operationName;
    }

    @Override
    public String toString() {
        return operationName;
    }
}
