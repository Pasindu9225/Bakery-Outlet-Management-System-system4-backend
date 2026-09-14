package com.plover.backerymanagmentsystem.pos.model;

/**
 * Enum representing the unit of measurement for products
 */
public enum ProductUnit {
    PIECES("pieces"),
    KG("kg"),
    LITERS("liters"),
    BOXES("boxes"),
    PACKETS("packets");

    private final String value;

    ProductUnit(String value) {
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
