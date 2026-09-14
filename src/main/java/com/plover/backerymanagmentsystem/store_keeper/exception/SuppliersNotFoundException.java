package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when no suppliers are found in the system.
 */
public class SuppliersNotFoundException extends RuntimeException {

    public SuppliersNotFoundException() {
        super("No suppliers found in the system");
    }

    public SuppliersNotFoundException(String message) {
        super(message);
    }
}
