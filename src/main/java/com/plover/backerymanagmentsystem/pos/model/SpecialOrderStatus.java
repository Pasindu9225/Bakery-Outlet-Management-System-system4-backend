package com.plover.backerymanagmentsystem.pos.model;

/**
 * Lifecycle states for a Special Order
 */
public enum SpecialOrderStatus {
    PENDING,            // Initial state after order creation
    ADVANCE_PAID,       // Advance payment received
    READY,              // Items prepared and ready for collection
    AWAITING_APPROVAL,  // Balance paid, awaiting manager closure approval
    COMPLETED,          // Order closed, inventory deducted, sale finalized
    CANCELLED           // Order cancelled before closure
}
