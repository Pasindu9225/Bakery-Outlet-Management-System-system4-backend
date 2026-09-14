package com.plover.backerymanagmentsystem.store_keeper.model;

/**
 * Enum representing the status of stock adjustment.
 */
public enum StockAdjustmentStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    DONE("done");

    private final String value;

    StockAdjustmentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
