package com.plover.backerymanagmentsystem.store_keeper.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StockAdjustmentStatusConverter implements AttributeConverter<StockAdjustmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(StockAdjustmentStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public StockAdjustmentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String lower = dbData.trim().toLowerCase();
        for (StockAdjustmentStatus s : StockAdjustmentStatus.values()) {
            if (s.getValue().equalsIgnoreCase(lower)) {
                return s;
            }
        }
        // Fallback: try enum name mapping
        try {
            return StockAdjustmentStatus.valueOf(lower.toUpperCase());
        } catch (IllegalArgumentException ex) {
            System.err.println("[ERROR] StockAdjustmentStatusConverter failed for dbData: " + dbData);
            ex.printStackTrace();
            throw ex;
        }
    }
}
