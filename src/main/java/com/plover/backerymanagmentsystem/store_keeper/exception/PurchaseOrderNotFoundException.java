package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a purchase order is not found.
 */
public class PurchaseOrderNotFoundException extends RuntimeException {

    public PurchaseOrderNotFoundException(Long purchaseOrderId) {
        super(String.format("Purchase order not found with ID: %d", purchaseOrderId));
    }

    public PurchaseOrderNotFoundException(String message) {
        super(message);
    }
}
