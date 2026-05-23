package com.marcura.exchangerate.service.exception;

public class FixerApiException extends RuntimeException {

    public FixerApiException(String message) {
        super(message);
    }

    public FixerApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
