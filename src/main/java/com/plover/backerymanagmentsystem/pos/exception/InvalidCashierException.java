package com.plover.backerymanagmentsystem.pos.exception;

/**
 * Exception thrown when cashier is invalid or not found
 */
public class InvalidCashierException extends SaleException {

    public InvalidCashierException(String cashierId) {
        super("Cashier with ID " + cashierId + " not found or inactive");
    }

    public InvalidCashierException(String message, Throwable cause) {
        super(message, cause);
    }
}
