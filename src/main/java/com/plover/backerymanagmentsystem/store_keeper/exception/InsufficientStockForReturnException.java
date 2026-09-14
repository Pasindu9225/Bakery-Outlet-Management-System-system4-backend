package com.plover.backerymanagmentsystem.store_keeper.exception;

import java.util.List;

/**
 * Exception thrown when return quantity exceeds available stock.
 */
public class InsufficientStockForReturnException extends RuntimeException {

    private final List<String> insufficientStockMaterials;

    public InsufficientStockForReturnException(List<String> insufficientStockMaterials) {
        super(String.format("Insufficient stock for return. Materials with insufficient stock: %s",
                String.join(", ", insufficientStockMaterials)));
        this.insufficientStockMaterials = insufficientStockMaterials;
    }

    public InsufficientStockForReturnException(String message, List<String> insufficientStockMaterials) {
        super(message);
        this.insufficientStockMaterials = insufficientStockMaterials;
    }

    public List<String> getInsufficientStockMaterials() {
        return insufficientStockMaterials;
    }
}
