package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderResponseDto;

/**
 * Service interface for purchase order creation operations.
 */
public interface PurchaseOrderCreationService {

    /**
     * Creates a new purchase order with the specified items.
     *
     * Validates: - Supplier exists - All raw materials exist - Supplier can
     * supply all requested materials - Delivery date is in the future
     *
     * Calculates: - Estimated costs from supplier-material relationships -
     * Total cost based on actual costs - Cost variance between actual and
     * estimated costs
     *
     * @param requestDto the purchase order creation request
     * @return CreatePurchaseOrderResponseDto with created order details
     * @throws SupplierNotFoundException if supplier doesn't exist
     * @throws RawMaterialNotFoundException if any raw material doesn't exist
     * @throws SupplierMaterialMismatchException if supplier can't supply any
     * material
     * @throws InvalidDeliveryDateException if delivery date is invalid
     */
    CreatePurchaseOrderResponseDto createPurchaseOrder(CreatePurchaseOrderRequestDto requestDto);

    /**
     * Creates multiple purchase orders in a single request.
     * Items for the same supplier and delivery date are grouped into one PO.
     *
     * @param request the bulk creation request
     * @return BulkCreatePurchaseOrderResponseDto with details of all created POs
     */
    com.plover.backerymanagmentsystem.store_keeper.dto.BulkCreatePurchaseOrderResponseDto createBulkPurchaseOrders(
            com.plover.backerymanagmentsystem.store_keeper.dto.BulkCreatePurchaseOrderRequestDto request);

    /**
     * Approves a pending purchase order and generates a Goods Received Note (GRN) for the storekeeper.
     *
     * @param poId the ID of the purchase order to approve
     * @return the updated purchase order details
     */
    CreatePurchaseOrderResponseDto approvePurchaseOrder(Long poId);
}
