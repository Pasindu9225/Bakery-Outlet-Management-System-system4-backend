package com.plover.backerymanagmentsystem.manager.service;

import com.plover.backerymanagmentsystem.manager.dto.ReservationResult;
import com.plover.backerymanagmentsystem.manager.model.SemiFinishedBatchInventory;

import java.util.List;

public interface SemiFinishedReservationService {

    /**
     * Reserve semi-finished stock for a production plan item using FIFO based on expiry dates.
     * Excludes expired batches automatically.
     */
    ReservationResult reserveSemiFinishedStock(Long productionPlanId, Long planItemId, Long productId, Long miniStoreId, Double requiredQty);

    /**
     * Release reserved stock allocations when a production plan is cancelled or deleted.
     */
    void releaseReservationsForPlan(Long productionPlanId);

    /**
     * Transition reservations to CONSUMED when production completes.
     */
    void consumeReservationsForPlanItem(Long planItemId);

    /**
     * Get available (unreserved & unexpired) stock for a semi-finished product.
     */
    Double getAvailableUnreservedStock(Long productId, Long miniStoreId);

    /**
     * Get all batch inventory items for a mini store (including expired ones).
     */
    List<SemiFinishedBatchInventory> getBatchInventoryForMiniStore(Long miniStoreId);

    /**
     * Auto-create a batch inventory record when worker completes production.
     */
    SemiFinishedBatchInventory createBatchInventoryOnProduction(Long productId, String productName, Double initialQty, Long miniStoreId);
}
