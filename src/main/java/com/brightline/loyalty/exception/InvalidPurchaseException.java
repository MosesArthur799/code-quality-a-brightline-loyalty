package com.brightline.loyalty.exception;

public class InvalidPurchaseException extends BrightlineLoyaltyException {

    private static final long serialVersionUID = 1L;

    public InvalidPurchaseException(String message) {
        super(message);
    }

    public InvalidPurchaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
