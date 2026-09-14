package com.plover.backerymanagmentsystem.core.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for data type conversions to handle database type mismatches.
 * Provides consistent conversion logic across the application.
 */
public final class DataTypeConverter {

    private DataTypeConverter() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts Double to BigDecimal with proper scaling for currency values.
     *
     * @param value the Double value to convert
     * @return BigDecimal with 2 decimal places, or null if input is null
     */
    public static BigDecimal doubleToBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Converts BigDecimal to Double for JPA entity fields.
     *
     * @param value the BigDecimal value to convert
     * @return Double value, or null if input is null
     */
    public static Double bigDecimalToDouble(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.doubleValue();
    }

    /**
     * Converts Double to BigDecimal with custom scale.
     *
     * @param value the Double value to convert
     * @param scale the number of decimal places
     * @return BigDecimal with specified scale, or null if input is null
     */
    public static BigDecimal doubleToBigDecimal(Double value, int scale) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * Safely converts Double to BigDecimal with validation.
     *
     * @param value the Double value to convert
     * @param fieldName the field name for error reporting
     * @return BigDecimal with 2 decimal places
     * @throws IllegalArgumentException if value is negative
     */
    public static BigDecimal safeDoubleToBigDecimal(Double value, String fieldName) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative: " + value);
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Converts Integer to Long for ID fields.
     *
     * @param value the Integer value to convert
     * @return Long value, or null if input is null
     */
    public static Long integerToLong(Integer value) {
        if (value == null) {
            return null;
        }
        return value.longValue();
    }

    /**
     * Converts Long to Integer for legacy compatibility.
     *
     * @param value the Long value to convert
     * @return Integer value, or null if input is null
     * @throws IllegalArgumentException if value exceeds Integer range
     */
    public static Integer longToInteger(Long value) {
        if (value == null) {
            return null;
        }
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Long value exceeds Integer range: " + value);
        }
        return value.intValue();
    }

    /**
     * Validates and converts quantity values with business logic checks.
     *
     * @param quantity the quantity to validate and convert
     * @param maxAllowed the maximum allowed quantity
     * @param fieldName the field name for error reporting
     * @return validated quantity as Double
     * @throws IllegalArgumentException if validation fails
     */
    public static Double validateQuantity(Double quantity, Double maxAllowed, String fieldName) {
        if (quantity == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive: " + quantity);
        }
        if (maxAllowed != null && quantity > maxAllowed) {
            throw new IllegalArgumentException(fieldName + " (" + quantity + ") exceeds available quantity (" + maxAllowed + ")");
        }
        return quantity;
    }
}
