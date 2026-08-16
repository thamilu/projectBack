package com.eshop.app.realtime.domain.exception;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a WebSocket connection ticket fails validation — malformed,
 * expired, tampered signature, or already redeemed (single-use violation).
 *
 * <p>Deliberately its own base rather than reusing
 * {@code user.domain.exception.AuthenticationException}, to avoid a
 * {@code realtime -> user} module dependency for something as small as an
 * exception base class.
 */
public class WsTicketException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public WsTicketException(String message) {
        super(message, "WS_TICKET_INVALID", HttpStatus.UNAUTHORIZED);
    }
}
