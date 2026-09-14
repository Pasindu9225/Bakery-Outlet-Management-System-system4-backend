package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.LowStockMaterialsResponseDto.PurchaseOrderInfo;

/**
 * Service interface for purchase order operations. Follows Single
 * Responsibility Principle by handling only purchase order queries.
 */
public interface PurchaseOrderService {

    /**
     * Checks if a purchase order exists for the given raw material.
     *
     * @param rawMaterialId the raw material ID to check
     * @return true if a purchase order exists for the material
     */
    boolean isPurchaseOrderCreatedForMaterial(Integer rawMaterialId);

    /**
     * Retrieves purchase order information for a specific raw material.
     *
     * @param rawMaterialId the raw material ID to get purchase order info for
     * @return PurchaseOrderInfo if exists, null otherwise
     */
    PurchaseOrderInfo getPurchaseOrderInfoForMaterial(Integer rawMaterialId);
}
