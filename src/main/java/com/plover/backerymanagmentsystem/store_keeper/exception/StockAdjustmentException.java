package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a stock adjustment operation fails due to invalid data
 * or business rules.
 */
public class StockAdjustmentException extends RuntimeException {

    public StockAdjustmentException(String message) {
        super(message);
    }

    public StockAdjustmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
