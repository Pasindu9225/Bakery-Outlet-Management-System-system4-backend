package com.plover.backerymanagmentsystem.pos.model;

/**
 * Enum representing the entry status for GTN items
 */
public enum EntryStatus {
    MANUAL("manual"),
    SYSTEM("system");

    private final String value;

    EntryStatus(String value) {
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
