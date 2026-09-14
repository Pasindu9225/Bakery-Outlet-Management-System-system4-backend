package com.plover.backerymanagmentsystem.store_keeper.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ReturnReasonConverter implements AttributeConverter<ReturnReason, String> {

    @Override
    public String convertToDatabaseColumn(ReturnReason attribute) {
        if (attribute == null) {
            return null;
        }
        // Persist with spaces as per DB enum
        return switch (attribute) {
            case EXPIRED -> "EXPIRED";
            case DAMAGED -> "DAMAGED";
            case WRONG_DELIVERY -> "WRONG DELIVERY";
            case QUALITY_ISSUES -> "QUALITY ISSUES";
            case OTHER -> "OTHER";
        };
    }

    @Override
    public ReturnReason convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase();
        return switch (normalized) {
            case "EXPIRED" -> ReturnReason.EXPIRED;
            case "DAMAGED" -> ReturnReason.DAMAGED;
            case "WRONG DELIVERY", "WRONG_DELIVERY" -> ReturnReason.WRONG_DELIVERY;
            case "QUALITY ISSUES", "QUALITY_ISSUES" -> ReturnReason.QUALITY_ISSUES;
            case "OTHER" -> ReturnReason.OTHER;
            default -> throw new IllegalArgumentException("Unknown ReturnReason: " + dbData);
        };
    }
}


