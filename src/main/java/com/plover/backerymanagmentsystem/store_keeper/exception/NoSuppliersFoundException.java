package com.plover.backerymanagmentsystem.store_keeper.exception;

/**
 * Exception thrown when no suppliers are found for a specific raw material.
 */
public class NoSuppliersFoundException extends RuntimeException {

    public NoSuppliersFoundException(Long rawMaterialId, String rawMaterialName) {
        super(String.format("No suppliers found for raw material: %s (ID: %d)", rawMaterialName, rawMaterialId));
    }
}
