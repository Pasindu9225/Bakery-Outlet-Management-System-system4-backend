package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Exception thrown when GTN-related operations fail
 */
public class GtnException extends RuntimeException {

    public GtnException(String message) {
        super(message);
    }

    public GtnException(String message, Throwable cause) {
        super(message, cause);
    }
}
