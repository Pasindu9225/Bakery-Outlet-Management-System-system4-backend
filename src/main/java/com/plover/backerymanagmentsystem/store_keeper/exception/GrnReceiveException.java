package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when GRN (Goods Receipt Note) receive operation fails.
 */
public class GrnReceiveException extends RuntimeException {

    public GrnReceiveException(String message) {
        super(message);
    }

    public GrnReceiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public GrnReceiveException(Throwable cause) {
        super(cause);
    }
}
