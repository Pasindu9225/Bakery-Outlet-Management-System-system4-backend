package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a GRN (Goods Receipt Note) is not found.
 */
public class GrnNotFoundException extends RuntimeException {

    public GrnNotFoundException(Long grnId) {
        super("GRN not found with ID: " + grnId);
    }

    public GrnNotFoundException(String message) {
        super(message);
    }

    public GrnNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
