package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when GRN (Goods Receipt Note) creation fails.
 */
public class GrnCreationException extends RuntimeException {

    public GrnCreationException(String message) {
        super(message);
    }

    public GrnCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrnCreationException(Throwable cause) {
        super(cause);
    }
}
