package com.eshop.app.user.domain.event;

public enum AuthEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_REFRESH_SUCCESS,
    TOKEN_REFRESH_FAILURE,
    OAUTH2_CALLBACK_SUCCESS,
    OAUTH2_CSRF_ATTEMPT,
    TOKEN_INTROSPECTION
}
