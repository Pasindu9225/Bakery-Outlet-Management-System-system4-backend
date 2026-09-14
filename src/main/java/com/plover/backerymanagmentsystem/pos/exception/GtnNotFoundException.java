package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Exception thrown when GTN is not found
 */
public class GtnNotFoundException extends GtnException {

    public GtnNotFoundException(String message) {
        super(message);
    }

    public GtnNotFoundException(Integer gtnId) {
        super("GTN with ID " + gtnId + " not found");
    }
}
