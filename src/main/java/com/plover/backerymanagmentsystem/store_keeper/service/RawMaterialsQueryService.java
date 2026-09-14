package com.plover.backerymanagmentsystem.store_keeper.service;

import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.dto.AllRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.PurchaseOrderRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialWithBatchesDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.SupplierRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.AggregatedStockResponseDto;

/**
 * Service interface for raw material query operations. Handles various raw
 * material retrieval scenarios.
 */
public interface RawMaterialsQueryService {

    List<RawMaterialWithBatchesDto> getAllRawMaterialsWithDetails();

    /**
     * Retrieves aggregated stock levels grouped by generic material.
     * 
     * @return List of AggregatedStockResponseDto
     */
    List<AggregatedStockResponseDto> getAggregatedStock();

    /**
     * Retrieves all raw materials in a specific purchase order with their
     * details.
     *
     * @param purchaseOrderId the purchase order ID to get raw materials for
     * @return PurchaseOrderRawMaterialsResponseDto containing purchase order
     * and raw material details
     * @throws PurchaseOrderNotFoundException if purchase order doesn't exist
     */
    PurchaseOrderRawMaterialsResponseDto getRawMaterialsByPurchaseOrderId(Long purchaseOrderId);

    /**
     * Retrieves all available raw materials with current stock greater than 0.
     *
     * @return AllRawMaterialsResponseDto containing list of available raw
     * materials
     * @throws RawMaterialsNotFoundException if no raw materials found with
     * stock > 0
     */
    AllRawMaterialsResponseDto getAllAvailableRawMaterials();

    /**
     * Retrieves all raw materials that a specific supplier can supply.
     *
     * @param supplierId the supplier ID to get raw materials for
     * @return SupplierRawMaterialsResponseDto containing supplier and raw
     * material details
     * @throws SupplierNotFoundException if supplier doesn't exist
     * @throws SupplierRawMaterialsNotFoundException if supplier exists but has
     * no raw materials
     */
    SupplierRawMaterialsResponseDto getRawMaterialsBySupplierId(Long supplierId);
}
