package com.plover.backerymanagmentsystem.store_keeper.model;

/**
 * Enumeration representing the reason for raw material return.
 */
public enum ReturnReason {
    /**
     * Material has expired and cannot be used.
     */
    EXPIRED,
    /**
     * Material is damaged and unusable.
     */
    DAMAGED,
    /**
     * Wrong material was delivered by supplier.
     */
    WRONG_DELIVERY,
    /**
     * Material does not meet quality standards.
     */
    QUALITY_ISSUES,
    /**
     * Other unspecified reason for return.
     */
    OTHER
}
