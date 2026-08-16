package com.eshop.app.user.domain.exception;

import org.springframework.http.HttpStatus;

public class OAuth2StateException extends AuthenticationException {
    public OAuth2StateException(String message) {
        super(message, "OAUTH2_STATE_ERROR", HttpStatus.BAD_REQUEST);
    }

    public OAuth2StateException(String message, Throwable cause) {
        super(message, "OAUTH2_STATE_ERROR", HttpStatus.BAD_REQUEST);
    }
}
