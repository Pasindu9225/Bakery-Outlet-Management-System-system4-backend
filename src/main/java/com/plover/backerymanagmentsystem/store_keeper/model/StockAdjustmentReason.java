package com.plover.backerymanagmentsystem.store_keeper.model;

/**
 * Enum representing the reason for stock adjustment.
 */
public enum StockAdjustmentReason {
    DAMAGE("Damage"),
    LOSS("Loss"),
    COUNTING_ERROR("Counting error"),
    EXPIRED("Expired"),
    OTHER("Other");

    private final String displayName;

    StockAdjustmentReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
