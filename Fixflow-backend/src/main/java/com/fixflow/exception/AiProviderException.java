package com.fixflow.exception;

public class AiProviderException extends RuntimeException {

    public enum Reason {
        NOT_CONFIGURED, AUTHENTICATION, RATE_LIMITED, TIMEOUT, UNAVAILABLE, REJECTED, BAD_RESPONSE
    }

    private final Reason reason;

    public AiProviderException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}