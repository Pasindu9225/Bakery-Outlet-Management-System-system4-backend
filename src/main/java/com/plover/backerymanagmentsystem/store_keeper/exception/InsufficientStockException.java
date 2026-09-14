package com.plover.backerymanagmentsystem.store_keeper.exception;

import java.util.List;

public class InsufficientStockException extends RuntimeException {

    private final List<String> insufficientMaterials;

    public InsufficientStockException(List<String> insufficientMaterials) {
        super(String.format("Insufficient stock for materials: %s", String.join(", ", insufficientMaterials)));
        this.insufficientMaterials = insufficientMaterials;
    }

    public List<String> getInsufficientMaterials() {
        return insufficientMaterials;
    }
}
