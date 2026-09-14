package com.plover.backerymanagmentsystem.pos.model;

/**
 * Enum representing the status of a GTN (Goods Transfer Note)
 */
public enum GtnStatus {
    RECEIVED("received"),
    PARTIALLY_RECEIVED("partially received"),
    OVER_RECEIVED("over received"),
    NOT_RECEIVED("not received");

    private final String value;

    GtnStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
