package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Exception thrown when there are issues with GTN processing
 */
public class GtnProcessingException extends GtnException {

    public GtnProcessingException(String message) {
        super(message);
    }

    public GtnProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
