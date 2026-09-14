package com.plover.backerymanagmentsystem.store_keeper.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StockAdjustmentReasonConverter implements AttributeConverter<StockAdjustmentReason, String> {

    @Override
    public String convertToDatabaseColumn(StockAdjustmentReason attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDisplayName();
    }

    @Override
    public StockAdjustmentReason convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim();
        // Match by display name as stored in DB
        for (StockAdjustmentReason reason : StockAdjustmentReason.values()) {
            if (reason.getDisplayName().equalsIgnoreCase(normalized)) {
                return reason;
            }
        }
        // Fallback: map some common variations
        String lower = normalized.toLowerCase();
        if ("damage".equals(lower)) {
            return StockAdjustmentReason.DAMAGE;
        }
        if ("loss".equals(lower)) {
            return StockAdjustmentReason.LOSS;
        }
        if ("counting error".equals(lower) || "counting_error".equals(lower)) {
            return StockAdjustmentReason.COUNTING_ERROR;
        }
        if ("expired".equals(lower)) {
            return StockAdjustmentReason.EXPIRED;
        }
        if ("other".equals(lower)) {
            return StockAdjustmentReason.OTHER;
        }
        System.err.println("[ERROR] StockAdjustmentReasonConverter failed for dbData: " + dbData);
        new Exception().printStackTrace();
        throw new IllegalArgumentException("Unknown StockAdjustmentReason: " + dbData);
    }
}
