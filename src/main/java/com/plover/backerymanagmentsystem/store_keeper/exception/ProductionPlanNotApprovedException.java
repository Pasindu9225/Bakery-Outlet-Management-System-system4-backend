package com.plover.backerymanagmentsystem.store_keeper.exception;

public class ProductionPlanNotApprovedException extends RuntimeException {

    public ProductionPlanNotApprovedException(Long planId, String currentStatus) {
        super(String.format("Production plan with ID %d is not approved. Current status: %s", planId, currentStatus));
    }
}
