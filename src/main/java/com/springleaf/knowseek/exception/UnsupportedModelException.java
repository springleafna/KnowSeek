package com.springleaf.knowseek.exception;

public class UnsupportedModelException extends RuntimeException {
    public UnsupportedModelException(String message) {
        super(message);
    }

    public UnsupportedModelException(String message, Throwable cause) {
        super(message, cause);
    }
}