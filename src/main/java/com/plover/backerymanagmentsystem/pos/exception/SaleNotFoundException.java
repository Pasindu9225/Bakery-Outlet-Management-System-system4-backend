package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Exception thrown when a sale is not found
 */
public class SaleNotFoundException extends RuntimeException {

    public SaleNotFoundException(Integer saleId) {
        super("Sale not found with ID: " + saleId);
    }

    public SaleNotFoundException(String message) {
        super(message);
    }

    public SaleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
