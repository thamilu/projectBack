package com.eshop.app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentGatewayException extends BusinessException {
    
    private static final long serialVersionUID = 1L;
    
    private final String gatewayErrorCode;
    private final String transactionId;

    public PaymentGatewayException(String message) {
        super(message, "PAYMENT_GATEWAY_ERROR", HttpStatus.BAD_GATEWAY);
        this.gatewayErrorCode = null;
        this.transactionId = null;
    }

    public PaymentGatewayException(String message, String gatewayErrorCode) {
        super(message, "PAYMENT_GATEWAY_ERROR", HttpStatus.BAD_GATEWAY);
        this.gatewayErrorCode = gatewayErrorCode;
        this.transactionId = null;
        addDetail("gatewayErrorCode", gatewayErrorCode);
    }

    public PaymentGatewayException(String message, String gatewayErrorCode, String transactionId) {
        super(message, "PAYMENT_GATEWAY_ERROR", HttpStatus.BAD_GATEWAY);
        this.gatewayErrorCode = gatewayErrorCode;
        this.transactionId = transactionId;
        addDetail("gatewayErrorCode", gatewayErrorCode);
        addDetail("transactionId", transactionId);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, "PAYMENT_GATEWAY_ERROR", cause);
        this.gatewayErrorCode = null;
        this.transactionId = null;
    }

    public PaymentGatewayException(String message, String gatewayErrorCode, Throwable cause) {
        super(message, "PAYMENT_GATEWAY_ERROR", cause);
        this.gatewayErrorCode = gatewayErrorCode;
        this.transactionId = null;
        addDetail("gatewayErrorCode", gatewayErrorCode);
    }
}

