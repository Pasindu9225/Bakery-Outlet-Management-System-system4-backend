package com.plover.backerymanagmentsystem.store_keeper.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.store_keeper.dto.AggregatedStockResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.AllRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.AllSuppliersResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ApproveRawMaterialReturnRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ApproveRawMaterialReturnResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkReduceRawMaterialStockRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkReduceRawMaterialStockResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkCreatePurchaseOrderRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkCreatePurchaseOrderResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateRawMaterialReturnRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateRawMaterialReturnResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateStockAdjustmentRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GetAllGrnsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GetAllRawMaterialReturnsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.LowStockMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.PartialReceiptResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionPlanMaterialSummaryResponse;
import com.plover.backerymanagmentsystem.store_keeper.dto.PurchaseOrderRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialRequirementsResponse;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialSuppliersResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialWithBatchesDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ReduceRawMaterialStockRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ReduceRawMaterialStockResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.StockAdjustmentResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.SupplierRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.UpdateRawMaterialReturnRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionPlanSummaryResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionCenterResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;
import com.plover.backerymanagmentsystem.store_keeper.service.GrnService;
import com.plover.backerymanagmentsystem.store_keeper.service.LowStockMaterialsService;
import com.plover.backerymanagmentsystem.store_keeper.service.MaterialIssuanceService;
import com.plover.backerymanagmentsystem.store_keeper.service.PurchaseOrderCreationService;
import com.plover.backerymanagmentsystem.store_keeper.service.PurchaseOrderQueryService;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialReturnService;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialStockService;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialSuppliersService;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialsQueryService;
import com.plover.backerymanagmentsystem.store_keeper.service.StockAdjustmentService;
import com.plover.backerymanagmentsystem.store_keeper.service.StoreKeeperProductionPlanService;
import com.plover.backerymanagmentsystem.store_keeper.service.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/STK/v1")
@RequiredArgsConstructor
@Slf4j
public class StoreKeeperController {

    private final StoreKeeperProductionPlanService storeKeeperProductionPlanService;
    private final MaterialIssuanceService materialIssuanceService;
    private final LowStockMaterialsService lowStockMaterialsService;
    private final RawMaterialSuppliersService rawMaterialSuppliersService;
    private final PurchaseOrderCreationService purchaseOrderCreationService;
    private final PurchaseOrderQueryService purchaseOrderQueryService;
    private final RawMaterialsQueryService rawMaterialsQueryService;
    private final RawMaterialStockService rawMaterialStockService;
    private final RawMaterialReturnService rawMaterialReturnService;
    private final GrnService grnService;
    private final SupplierService supplierService;
    private final StockAdjustmentService stockAdjustmentService;

    private final com.plover.backerymanagmentsystem.store_keeper.service.ReturnProcessingService returnProcessingService;
    private final com.plover.backerymanagmentsystem.store_keeper.service.BomService bomService;

    @GetMapping("/approved-plans/material-totals")
    public ResponseEntity<RawMaterialRequirementsResponse> getApprovedProductionPlanMaterials() {
        return ResponseEntity.ok(storeKeeperProductionPlanService.getApprovedProductionPlanMaterials());

    }

    @GetMapping("/approved-plans/material-summary")
    public ResponseEntity<ProductionPlanSummaryResponseDto> getProductionPlanWiseMaterialSummary() {
        return ResponseEntity.ok(storeKeeperProductionPlanService.getProductionPlanSummary());
    }

    @GetMapping("/production-centers")
    public ResponseEntity<List<ProductionCenterResponseDto>> getAllProductionCenters() {
        log.info("Request received to get all production centers");
        return ResponseEntity.ok(storeKeeperProductionPlanService.getAllProductionCenters());
    }

    @GetMapping("/bom/{productId}")
    public ResponseEntity<com.plover.backerymanagmentsystem.store_keeper.dto.BomResponseDto> getBillOfMaterials(
            @PathVariable Long productId) {
        return ResponseEntity.ok(bomService.getBomByParentProductId(productId));
    }

    @GetMapping("/bom/{productId}/tree")
    public ResponseEntity<com.plover.backerymanagmentsystem.store_keeper.dto.BomTreeResponseDto> getBillOfMaterialsTree(
            @PathVariable Long productId) {
        return ResponseEntity.ok(bomService.getBomTree(productId));
    }

    @PostMapping("/approved-plans/issue-plan")
    public ResponseEntity<IssueMaterialsResponseDto> issueMaterials(
            @Valid @RequestBody com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto request) {
        return ResponseEntity.ok(materialIssuanceService.issueMaterialsForProductionPlan(request));
    }

    @GetMapping("/materials/get-low-stock")
    public ResponseEntity<LowStockMaterialsResponseDto> getLowStockMaterials() {
        LowStockMaterialsResponseDto lowStockMaterials = lowStockMaterialsService.getLowStockMaterials();
        return ResponseEntity.ok(lowStockMaterials);
    }

    @GetMapping("/suppliers/by-material/{rawMaterialId}")
    public ResponseEntity<RawMaterialSuppliersResponseDto> getSuppliersByRawMaterial(
            @PathVariable Long rawMaterialId) {
        log.info("Retrieving suppliers for raw material ID: {}", rawMaterialId);
        RawMaterialSuppliersResponseDto suppliers = rawMaterialSuppliersService.getSuppliersByRawMaterial(rawMaterialId);
        return ResponseEntity.ok(suppliers);
    }

    @GetMapping("/suppliers")
    public ResponseEntity<AllSuppliersResponseDto> getAllSuppliers() {
        log.info("Retrieving all suppliers from the system");
        AllSuppliersResponseDto response = supplierService.getAllSuppliers();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase-orders/create")
    public ResponseEntity<BulkCreatePurchaseOrderResponseDto> createPurchaseOrder(
            @Valid @RequestBody BulkCreatePurchaseOrderRequestDto request) {
        log.info("Processing bulk purchase order creation for {} entries",
                request.getPurchaseOrders().size());
        BulkCreatePurchaseOrderResponseDto response = purchaseOrderCreationService.createBulkPurchaseOrders(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/materials/all")
    public ResponseEntity<java.util.List<RawMaterialWithBatchesDto>> getAllRawMaterialsWithDetails() {
        log.info("Request received to get all raw materials with details");
        java.util.List<RawMaterialWithBatchesDto> materials = rawMaterialsQueryService.getAllRawMaterialsWithDetails();
        log.info("Returning {} raw materials", materials.size());
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/purchase-orders")
    public ResponseEntity<com.plover.backerymanagmentsystem.store_keeper.dto.DetailedPurchaseOrderResponseDto> getAllPurchaseOrders() {
        log.info("Retrieving all purchase orders with detailed information");
        com.plover.backerymanagmentsystem.store_keeper.dto.DetailedPurchaseOrderResponseDto response = purchaseOrderQueryService.getAllDetailedPurchaseOrders();
        log.info("Successfully retrieved {} purchase orders with details", response.getTotalCount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/purchase-orders/partial-receipts")
    public ResponseEntity<PartialReceiptResponseDto> getPartialReceiptSummary() {
        log.info("Retrieving partial receipt summary for purchase orders");
        PartialReceiptResponseDto response = grnService.getPartialReceiptSummary();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/purchase-orders/{poId}/raw-materials")
    public ResponseEntity<PurchaseOrderRawMaterialsResponseDto> getRawMaterialsByPurchaseOrderId(
            @PathVariable Long poId) {
        log.info("Retrieving raw materials for purchase order ID: {}", poId);
        PurchaseOrderRawMaterialsResponseDto response = rawMaterialsQueryService.getRawMaterialsByPurchaseOrderId(poId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/materials/available")
    public ResponseEntity<AllRawMaterialsResponseDto> getAllAvailableRawMaterials() {
        log.info("Retrieving all available raw materials with current stock > 0");
        AllRawMaterialsResponseDto response = rawMaterialsQueryService.getAllAvailableRawMaterials();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/materials/aggregated-stock")
    public ResponseEntity<List<AggregatedStockResponseDto>> getAggregatedStock() {
        log.info("Request received to get aggregated raw material stock");
        return ResponseEntity.ok(rawMaterialsQueryService.getAggregatedStock());
    }

    /**
     * Reduces stock for a single raw material.
     *
     * @param request the stock reduction request containing material ID and
     * quantity
     * @return the stock reduction response with updated stock information
     */
    @PostMapping("/materials/reduce-stock")
    public ResponseEntity<ReduceRawMaterialStockResponseDto> reduceRawMaterialStock(
            @Valid @RequestBody ReduceRawMaterialStockRequestDto request) {
        log.info("Reducing stock for material ID: {} by quantity: {}", request.getMaterialId(), request.getQuantity());
        ReduceRawMaterialStockResponseDto response = rawMaterialStockService.reduceStock(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reduces stock for multiple raw materials in a single request. Individual
     * failures are handled gracefully without affecting successful operations.
     *
     * @param request the bulk stock reduction request
     * @return the bulk stock reduction response with successful and failed
     * operations
     */
    @PostMapping("/materials/bulk-reduce-stock")
    public ResponseEntity<BulkReduceRawMaterialStockResponseDto> bulkReduceRawMaterialStock(
            @Valid @RequestBody BulkReduceRawMaterialStockRequestDto request) {
        log.info("Processing bulk stock reduction for {} materials", request.getMaterialReductions().size());
        BulkReduceRawMaterialStockResponseDto response = rawMaterialStockService.bulkReduceStock(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all GRNs (Goods Receipt Notes) with their basic information and item
     * counts. Returns comprehensive list of all GRNs including supplier
     * information and number of items. URL: GET /STK/v1/grns
     */
    @GetMapping("/grns")
    public ResponseEntity<GetAllGrnsResponseDto> getAllGrns() {
        log.info("Retrieving all GRNs with item counts");

        GetAllGrnsResponseDto response = grnService.getAllGrns();

        log.info("Successfully retrieved {} GRNs", response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    /**
     * Processes the receipt of goods for a GRN (Goods Receipt Note). Updates
     * GRN status, received quantities, and raw material stocks.
     *
     * @param grnId the GRN ID
     * @param request the receive request containing items and status
     * @return the response with updated information and any errors
     */
    @PostMapping("/grn/{grnId}/receive")
    public ResponseEntity<GrnReceiveResponseDto> receiveGoods(
            @PathVariable Long grnId,
            @Valid @RequestBody GrnReceiveRequestDto request) {
        log.info("Processing goods receipt for GRN ID: {} with {} items", grnId, request.getItems().size());
        GrnReceiveResponseDto response = grnService.receiveGoods(grnId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get raw materials by supplier ID Returns supplier information along with
     * their raw materials URL: GET /STK/v1/suppliers/{supplierId}/raw-materials
     */
    @GetMapping("/suppliers/{supplierId}/raw-materials")
    public ResponseEntity<SupplierRawMaterialsResponseDto> getRawMaterialsBySupplier(
            @PathVariable Long supplierId) {
        log.info("Request received to get raw materials for supplier ID: {}", supplierId);

        SupplierRawMaterialsResponseDto response = rawMaterialsQueryService.getRawMaterialsBySupplierId(supplierId);

        log.info("Successfully retrieved raw materials for supplier ID: {} with {} materials",
                supplierId, response.getRawMaterials().size());

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new raw material return Validates supplier, materials, and stock
     * before creating the return URL: POST /STK/v1/returns/create
     */
    @PostMapping("/returns/create")
    public ResponseEntity<CreateRawMaterialReturnResponseDto> createRawMaterialReturn(
            @Valid @RequestBody CreateRawMaterialReturnRequestDto request) {
        log.info("Creating raw material return for supplier ID: {} with {} items",
                request.getSupplierId(), request.getReturnItems().size());

        CreateRawMaterialReturnResponseDto response = rawMaterialReturnService.createRawMaterialReturn(request);

        log.info("Successfully created raw material return with ID: {} for supplier: {} with total cost: {}",
                response.getReturnId(), response.getSupplierName(), response.getTotalCost());

        return ResponseEntity.ok(response);
    }

    /**
     * Get raw material return details by ID URL: GET /STK/v1/returns/{returnId}
     */
    @GetMapping("/returns/{returnId}")
    public ResponseEntity<CreateRawMaterialReturnResponseDto> getRawMaterialReturn(
            @PathVariable Long returnId) {
        log.info("Retrieving raw material return with ID: {}", returnId);

        CreateRawMaterialReturnResponseDto response = rawMaterialReturnService.getRawMaterialReturnById(returnId);

        log.info("Successfully retrieved raw material return: {} for supplier: {}",
                response.getReturnId(), response.getSupplierName());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all raw material returns with item details URL: GET /STK/v1/returns
     */
    @GetMapping("/returns")
    public ResponseEntity<GetAllRawMaterialReturnsResponseDto> getAllRawMaterialReturns() {
        log.info("Retrieving all raw material returns");
        GetAllRawMaterialReturnsResponseDto response = rawMaterialReturnService.getAllRawMaterialReturns();
        log.info("Successfully retrieved {} returns", response.getTotalCount());
        return ResponseEntity.ok(response);
    }

    /**
     * Approve raw material return items Updates return item status to APPROVED
     * and reduces stock in raw materials table URL: PUT /STK/v1/returns/approve
     */
    @PutMapping("/returns/approve")
    public ResponseEntity<ApproveRawMaterialReturnResponseDto> approveReturnItems(
            @Valid @RequestBody ApproveRawMaterialReturnRequestDto request) {
        log.info("Approving {} return items", request.getReturnItemIds().size());

        ApproveRawMaterialReturnResponseDto response = rawMaterialReturnService.approveReturnItems(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing raw material return note and its item details. URL:
     * PUT /STK/v1/returns/{returnId}
     */
    @PutMapping("/returns/{returnId}")
    public ResponseEntity<CreateRawMaterialReturnResponseDto> updateRawMaterialReturn(
            @PathVariable Long returnId,
            @Valid @RequestBody UpdateRawMaterialReturnRequestDto request) {
        log.info("Updating return note {}", returnId);

        if (request.getReturnId() == null || !request.getReturnId().equals(returnId)) {
            throw new IllegalArgumentException("Path returnId and request.returnId must match");
        }
        CreateRawMaterialReturnResponseDto response = rawMaterialReturnService.updateRawMaterialReturn(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a return note and deduct stock for returned raw materials. Body
     * matches the provided structure. For items with status APPROVED, it
     * updates item status to RETURNED and deducts current stock in raw
     * materials.
     */
    @PutMapping("/return-materials")
    public ResponseEntity<Void> returnMaterials(
            @Valid @RequestBody com.plover.backerymanagmentsystem.store_keeper.dto.ReturnMaterialsRequestDto request) {
        log.info("Processing return materials request");
        returnProcessingService.processReturn(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Create a new stock adjustment record. This endpoint allows store keepers
     * to record stock adjustments for various reasons such as damage, loss,
     * counting errors, expiration, etc. URL: POST /STK/v1/stock-adjustments
     */
    @PostMapping("/stock-adjustments")
    public ResponseEntity<StockAdjustmentResponseDto> createStockAdjustment(
            @Valid @RequestBody CreateStockAdjustmentRequestDto request) {
        log.info("Creating stock adjustment for raw material ID: {} with quantity: {}",
                request.getRawMaterialId(), request.getAdjustmentQty());

        StockAdjustmentResponseDto response = stockAdjustmentService.createStockAdjustment(request);

        log.info("Successfully created stock adjustment with ID: {} for raw material: {}",
                response.getId(), response.getRawMaterialName());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all stock adjustments with relevant raw material names. URL: GET
     * /STK/v1/stock-adjustments
     */
    @GetMapping("/stock-adjustments")
    public ResponseEntity<List<StockAdjustmentResponseDto>> getAllStockAdjustments() {
        log.info("Retrieving all stock adjustments");
        try {
            List<StockAdjustmentResponseDto> response = stockAdjustmentService.getAllStockAdjustments();
            log.info("Successfully retrieved {} stock adjustments", response.size());
            for (StockAdjustmentResponseDto dto : response) {
                log.debug("StockAdjustmentResponseDto: id={}, status={}, reason={}", dto.getId(), dto.getStatus());
            }
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error in getAllStockAdjustments endpoint", ex);
            throw ex;
        }
    }

    /**
     * Get all stock adjustments for a specific raw material. URL: GET
     * /STK/v1/stock-adjustments/raw-material/{rawMaterialId}
     */
    @GetMapping("/stock-adjustments/raw-material/{rawMaterialId}")
    public ResponseEntity<List<StockAdjustmentResponseDto>> getStockAdjustmentsByRawMaterial(
            @PathVariable Long rawMaterialId) {
        log.info("Retrieving stock adjustments for raw material ID: {}", rawMaterialId);

        List<StockAdjustmentResponseDto> response = stockAdjustmentService.getStockAdjustmentsByRawMaterial(rawMaterialId);

        log.info("Successfully retrieved {} stock adjustments for raw material ID: {}",
                response.size(), rawMaterialId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all pending stock adjustments. URL: GET
     * /STK/v1/stock-adjustments/pending
     */
    @GetMapping("/stock-adjustments/pending")
    public ResponseEntity<List<StockAdjustmentResponseDto>> getAllPendingStockAdjustments() {
        log.info("Retrieving all pending stock adjustments");

        List<StockAdjustmentResponseDto> response = stockAdjustmentService.getAllPendingAdjustments();

        log.info("Successfully retrieved {} pending stock adjustments", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get stock adjustments by status. URL: GET
     * /STK/v1/stock-adjustments/status/{status}
     */
    @GetMapping("/stock-adjustments/status/{status}")
    public ResponseEntity<List<StockAdjustmentResponseDto>> getStockAdjustmentsByStatus(
            @PathVariable StockAdjustmentStatus status) {
        log.info("Retrieving stock adjustments with status: {}", status);

        List<StockAdjustmentResponseDto> response = stockAdjustmentService.getStockAdjustmentsByStatus(status);

        log.info("Successfully retrieved {} stock adjustments with status: {}", response.size(), status);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific stock adjustment by ID. URL: GET
     * /STK/v1/stock-adjustments/{adjustmentId}
     */
    @GetMapping("/stock-adjustments/{adjustmentId}")
    public ResponseEntity<StockAdjustmentResponseDto> getStockAdjustmentById(
            @PathVariable Long adjustmentId) {
        log.info("Retrieving stock adjustment with ID: {}", adjustmentId);

        StockAdjustmentResponseDto response = stockAdjustmentService.getStockAdjustmentById(adjustmentId);

        log.info("Successfully retrieved stock adjustment: {} for raw material: {}",
                response.getId(), response.getRawMaterialName());

        return ResponseEntity.ok(response);
    }

    /**
     * Get real-time dashboard statistics for the Storekeeper module.
     * Includes pending manager requests, recent transactions, stock alerts, and worker request counts.
     *
     * @return ResponseEntity containing dashboard statistics
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<com.plover.backerymanagmentsystem.store_keeper.dto.StorekeeperDashboardStatsResponseDto> getDashboardStats() {
        log.info("Received request for Storekeeper dashboard stats");
        return ResponseEntity.ok(storeKeeperProductionPlanService.getDashboardStats());
    }
}
