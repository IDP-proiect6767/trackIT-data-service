package com.trackit.data.exception;

public class MissingUserContextException extends RuntimeException {
    public MissingUserContextException() {
        super("X-User-Id header missing or invalid");
    }
}
