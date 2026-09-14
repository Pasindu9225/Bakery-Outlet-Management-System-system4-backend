package com.plover.backerymanagmentsystem.store_keeper.exception;

public class MaterialsAlreadyIssuedException extends RuntimeException {

    public MaterialsAlreadyIssuedException(Long planId) {
        super(String.format("Materials for production plan ID %d have already been issued", planId));
    }
}
