package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a supplier cannot supply one or more requested
 * materials.
 */
public class SupplierMaterialMismatchException extends RuntimeException {

    public SupplierMaterialMismatchException(Long supplierId, String supplierName, Long rawMaterialId, String materialName) {
        super(String.format("Supplier '%s' (ID: %d) cannot supply material '%s' (ID: %d). No supplier-material relationship exists.",
                supplierName, supplierId, materialName, rawMaterialId));
    }

    public SupplierMaterialMismatchException(String message) {
        super(message);
    }
}
