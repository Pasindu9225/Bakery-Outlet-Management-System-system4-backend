package com.plover.backerymanagmentsystem.store_keeper.model;

/**
 * Enumeration representing the status of a Goods Receipt Note (GRN).
 */
public enum GrnStatus {
    /**
     * GRN has been created but no goods have been received yet.
     */
    PENDING,
    /**
     * Some goods have been received but not all items in the GRN.
     */
    PARTIAL,
    /**
     * All goods in the GRN have been fully received.
     */
    RECEIVED,
    /**
     * The GRN has been cancelled and will not be received.
     */
    CANCELLED
}
