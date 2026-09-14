package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Base exception for sale-related operations
 */
public class SaleException extends RuntimeException {

    public SaleException(String message) {
        super(message);
    }

    public SaleException(String message, Throwable cause) {
        super(message, cause);
    }
}