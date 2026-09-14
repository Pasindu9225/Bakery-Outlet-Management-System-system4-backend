package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a supplier exists but has no raw materials associated
 * with them.
 */
public class SupplierRawMaterialsNotFoundException extends RuntimeException {

    public SupplierRawMaterialsNotFoundException(Long supplierId, String supplierName) {
        super(String.format("No raw materials found for supplier '%s' (ID: %d)", supplierName, supplierId));
    }

    public SupplierRawMaterialsNotFoundException(String message) {
        super(message);
    }
}
