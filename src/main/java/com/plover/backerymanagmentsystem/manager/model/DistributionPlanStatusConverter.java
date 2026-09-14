package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DistributionPlanStatusConverter implements AttributeConverter<DistributionPlanStatus, String> {

    @Override
    public String convertToDatabaseColumn(DistributionPlanStatus status) {
        if (status == null) {
            return null;
        }
        // Persist using the enum's value string
        return status.getValue();
    }

    @Override
    public DistributionPlanStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        // Accept variations from existing data (e.g., with space)
        String normalized = dbData.trim().toLowerCase();
        if ("received".equals(normalized)) {
            return DistributionPlanStatus.RECEIVED;
        }
        if ("not-received".equals(normalized) || "not- received".equals(normalized) || "not received".equals(normalized)) {
            return DistributionPlanStatus.NOT_RECEIVED;
        }
        // Fallback: try enum name mapping if DB accidentally stores enum names
        try {
            return DistributionPlanStatus.valueOf(normalized.toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            throw ex;
        }
    }
}


