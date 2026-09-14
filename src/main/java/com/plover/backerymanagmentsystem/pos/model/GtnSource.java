package com.plover.backerymanagmentsystem.pos.model;

/**
 * Enum representing the source of a GTN (Goods Transfer Note)
 */
public enum GtnSource {
    BAKERY("bakery"),
    STORE("store"),
    KITCHEN("kitchen"),
    PRODUCTION("production"),
    OUTLET("outlet"),
    UNDEFINED("undefined");

    private final String value;

    GtnSource(String value) {
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
