package com.eshop.app.user.domain.exception;

import org.springframework.http.HttpStatus;

public class OAuth2CsrfException extends AuthenticationException {
    public OAuth2CsrfException(String message) {
        super(message, "OAUTH2_CSRF_ERROR", HttpStatus.FORBIDDEN);
    }
}
