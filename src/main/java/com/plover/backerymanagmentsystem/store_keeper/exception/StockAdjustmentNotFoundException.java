package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a stock adjustment is not found in the system.
 */
public class StockAdjustmentNotFoundException extends RuntimeException {

    public StockAdjustmentNotFoundException(Long adjustmentId) {
        super(String.format("Stock adjustment not found with ID: %d", adjustmentId));
    }

    public StockAdjustmentNotFoundException(String message) {
        super(message);
    }
}
