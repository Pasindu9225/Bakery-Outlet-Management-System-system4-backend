package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when a raw material is not found in the system.
 */
public class RawMaterialNotFoundException extends RuntimeException {

    public RawMaterialNotFoundException(Long rawMaterialId) {
        super(String.format("Raw material not found with ID: %d", rawMaterialId));
    }

    public RawMaterialNotFoundException(String message) {
        super(message);
    }
}
