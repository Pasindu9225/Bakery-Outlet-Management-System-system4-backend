package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a supplier is not found.
 */
public class SupplierNotFoundException extends RuntimeException {

    public SupplierNotFoundException(Long supplierId) {
        super(String.format("Supplier not found with ID: %d", supplierId));
    }

    public SupplierNotFoundException(String message) {
        super(message);
    }
}
