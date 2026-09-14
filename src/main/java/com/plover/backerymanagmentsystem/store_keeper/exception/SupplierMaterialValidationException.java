package com.plover.backerymanagmentsystem.store_keeper.exception;

import java.util.List;

/**
 * Exception thrown when supplier cannot supply the specified raw materials.
 */
public class SupplierMaterialValidationException extends RuntimeException {

    private final List<String> invalidMaterials;

    public SupplierMaterialValidationException(Long supplierId, List<String> invalidMaterials) {
        super(String.format("Supplier with ID %d cannot supply the following materials: %s",
                supplierId, String.join(", ", invalidMaterials)));
        this.invalidMaterials = invalidMaterials;
    }

    public SupplierMaterialValidationException(String message, List<String> invalidMaterials) {
        super(message);
        this.invalidMaterials = invalidMaterials;
    }

    public List<String> getInvalidMaterials() {
        return invalidMaterials;
    }
}
