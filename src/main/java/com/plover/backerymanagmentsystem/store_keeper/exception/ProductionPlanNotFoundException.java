package com.plover.backerymanagmentsystem.store_keeper.exception;

public class ProductionPlanNotFoundException extends RuntimeException {

    public ProductionPlanNotFoundException(Long planId) {
        super(String.format("Production plan not found with ID: %d", planId));
    }
}
