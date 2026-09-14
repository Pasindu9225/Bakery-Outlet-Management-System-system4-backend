package com.plover.backerymanagmentsystem.store_keeper.service;

import java.util.List;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.store_keeper.dto.GetAllGrnsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.PartialReceiptResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;

/**
 * Service interface for GRN (Goods Receipt Note) operations.
 */
public interface GrnService {

    /**
     * Creates a GRN with items for a purchase order. This method is called when
     * a purchase order is created to set up the corresponding GRN.
     *
     * @param purchaseOrder the purchase order for which to create the GRN
     * @return the created GRN with its items
     * @throws GrnCreationException if GRN creation fails
     */
    Grn createGrnForPurchaseOrder(PurchaseOrder purchaseOrder);

    /**
     * Processes the receipt of goods for a GRN. Updates GRN status, received
     * quantities, and raw material stocks.
     *
     * @param grnId the GRN ID
     * @param request the receive request containing items and status
     * @return the response with updated information and any errors
     * @throws GrnNotFoundException if GRN is not found
     * @throws GrnReceiveException if receive operation fails
     */
    GrnReceiveResponseDto receiveGoods(Long grnId, GrnReceiveRequestDto request);

    /**
     * Finds a GRN by purchase order ID.
     *
     * @param poId the purchase order ID
     * @return the GRN if found, null otherwise
     */
    Grn findByPurchaseOrderId(Long poId);

    /**
     * Finds all GRNs for a supplier.
     *
     * @param supplierId the supplier ID
     * @return list of GRNs for the supplier
     */
    List<Grn> findBySupplier(Long supplierId);

    /**
     * Checks if a GRN exists for a purchase order.
     *
     * @param poId the purchase order ID
     * @return true if GRN exists, false otherwise
     */
    boolean existsForPurchaseOrder(Long poId);

    /**
     * Retrieves all GRNs with their item counts and supplier information.
     * Returns comprehensive GRN data including the number of items in each GRN.
     *
     * @return response DTO containing list of all GRNs with their details and
     * item counts
     */
    GetAllGrnsResponseDto getAllGrns();

    /**
     * Retrieves purchase orders that have outstanding quantities based on GRN
     * receipts.
     *
     * @return partial receipt summary grouped by purchase order
     */
    PartialReceiptResponseDto getPartialReceiptSummary();
}
