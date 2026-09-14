package com.plover.backerymanagmentsystem.manager.model;

public enum DistributionPlanStatus {
    RECEIVED("received"),
    NOT_RECEIVED("not-received");
    
    private final String value;
    
    DistributionPlanStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    // Helper method to convert from string to enum
    public static DistributionPlanStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        
        // Normalize common variations (case/spacing/hyphen)
        String normalizedStatus = status.trim().toLowerCase();
        if ("received".equals(normalizedStatus)) {
            return RECEIVED;
        } else if ("not-received".equals(normalizedStatus) || "not- received".equals(normalizedStatus) || "not received".equals(normalizedStatus)) {
            return NOT_RECEIVED;
        }
        
        throw new IllegalArgumentException("Invalid status value: " + status);
    }
}
