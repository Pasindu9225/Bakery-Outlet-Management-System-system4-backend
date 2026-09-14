package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a raw material return is not found.
 */
public class RawMaterialReturnNotFoundException extends RuntimeException {

    public RawMaterialReturnNotFoundException(Long returnId) {
        super(String.format("Raw material return not found with ID: %d", returnId));
    }

    public RawMaterialReturnNotFoundException(String message) {
        super(message);
    }

    public RawMaterialReturnNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
