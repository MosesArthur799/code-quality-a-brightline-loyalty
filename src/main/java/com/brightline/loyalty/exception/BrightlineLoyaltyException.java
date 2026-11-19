package com.brightline.loyalty.exception;

public class BrightlineLoyaltyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BrightlineLoyaltyException(String message) {
        super(message);
    }

    public BrightlineLoyaltyException(String message, Throwable cause) {
        super(message, cause);
    }
}
