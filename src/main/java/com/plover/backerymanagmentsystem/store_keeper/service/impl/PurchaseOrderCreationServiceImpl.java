package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderRequestDto.PurchaseOrderItemRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderResponseDto.PurchaseOrderItemResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkCreatePurchaseOrderRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkCreatePurchaseOrderResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.GrnCreationException;
import com.plover.backerymanagmentsystem.store_keeper.exception.InvalidDeliveryDateException;
import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.SupplierNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialSupplier;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialSupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.GrnService;
import com.plover.backerymanagmentsystem.store_keeper.service.PurchaseOrderCreationService;
import com.plover.backerymanagmentsystem.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of PurchaseOrderCreationService. Handles purchase order
 * creation with comprehensive business logic, validations, and cost
 * calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PurchaseOrderCreationServiceImpl implements PurchaseOrderCreationService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final SupplierRepository supplierRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final RawMaterialSupplierRepository rawMaterialSupplierRepository;
    private final GrnService grnService;
    private final NotificationService notificationService;

    @Override
    public BulkCreatePurchaseOrderResponseDto createBulkPurchaseOrders(BulkCreatePurchaseOrderRequestDto request) {
        log.info("Processing bulk purchase order creation for {} initial requests", request.getPurchaseOrders().size());

        // Group by both supplierId and estimatedDeliveryDate to ensure items for the same supplier on the same day are in one PO
        java.util.Map<String, CreatePurchaseOrderRequestDto> groupedOrders = new java.util.LinkedHashMap<>();

        for (CreatePurchaseOrderRequestDto orderDto : request.getPurchaseOrders()) {
            String compositeKey = orderDto.getSupplierId() + "_" + orderDto.getEstimatedDeliveryDate();
            
            if (groupedOrders.containsKey(compositeKey)) {
                log.info("Merging items for supplier ID: {} and delivery date: {}", orderDto.getSupplierId(), orderDto.getEstimatedDeliveryDate());
                CreatePurchaseOrderRequestDto existingOrder = groupedOrders.get(compositeKey);
                
                // Add all items from the new request to the existing one
                List<PurchaseOrderItemRequestDto> combinedItems = new ArrayList<>(existingOrder.getItems());
                combinedItems.addAll(orderDto.getItems());
                existingOrder.setItems(combinedItems);
            } else {
                // Clone/Create new to avoid modifying request objects directly if needed, but here we can just put
                groupedOrders.put(compositeKey, orderDto);
            }
        }

        List<CreatePurchaseOrderResponseDto> responses = new ArrayList<>();
        for (CreatePurchaseOrderRequestDto mergedOrder : groupedOrders.values()) {
            responses.add(createPurchaseOrder(mergedOrder));
        }

        return BulkCreatePurchaseOrderResponseDto.builder()
                .createdPurchaseOrders(responses)
                .totalOrdersCreated(responses.size())
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public CreatePurchaseOrderResponseDto createPurchaseOrder(CreatePurchaseOrderRequestDto requestDto) {
        log.info("Creating purchase order for supplier ID: {}", requestDto.getSupplierId());

        // Step 1: Validate inputs
        validatePurchaseOrderRequest(requestDto);

        // Step 2: Get supplier details
        Supplier supplier = getSupplierById(requestDto.getSupplierId());

        // Step 3: Validate supplier-material relationships and calculate costs
        List<PurchaseOrderItemData> itemsData = validateAndCalculateItems(requestDto, supplier);

        // Step: Determine if any item exceeds max stock level
        // Condition: (current stock + required quantity) > max_stock_level
        boolean exceedsMaxStock = itemsData.stream()
                .anyMatch(item -> {
                    Double currentStock = item.getRawMaterial().getCurrentStock() != null
                            ? item.getRawMaterial().getCurrentStock() : 0.0;
                    Double maxStock = item.getRawMaterial().getMaxStockLevel();
                    Double requiredQty = item.getRequiredQty() != null ? item.getRequiredQty() : 0.0;
                    
                    boolean exceeds = maxStock != null && (currentStock + requiredQty) > maxStock;
                    
                    log.info("Threshold Check for Material ID {}: Current Stock={}, Required Qty={}, Max Stock={}, Exceeds={}", 
                            item.getRawMaterial().getId(), currentStock, requiredQty, maxStock, exceeds);
                    
                    return exceeds;
                });

        String status = exceedsMaxStock ? "PENDING" : "APPROVED";

        // Step 4: Calculate totals
        BigDecimal totalActualCost = calculateTotalActualCost(itemsData);
        BigDecimal totalEstimatedCost = calculateTotalEstimatedCost(itemsData);

        // Step 5: Create and save purchase order
        PurchaseOrder purchaseOrder = createPurchaseOrderEntity(requestDto, supplier, totalActualCost, itemsData.size(), status);
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        // Step 6: Create and save purchase order items
        List<PurchaseOrderItem> savedItems = createAndSavePurchaseOrderItems(savedPurchaseOrder, itemsData);

        // Set the items in the purchase order for GRN creation
        savedPurchaseOrder.setPurchaseOrderItems(savedItems);

        // Step 7: Create GRN ONLY for approved orders (those that don't exceed max stock level)
        if ("APPROVED".equals(status)) {
            try {
                grnService.createGrnForPurchaseOrder(savedPurchaseOrder);
                log.info("Successfully created GRN for purchase order ID: {}", savedPurchaseOrder.getPoId());
            } catch (GrnCreationException e) {
                log.error("Failed to create GRN for purchase order ID: {}. Rolling back transaction.",
                        savedPurchaseOrder.getPoId(), e);
                throw new RuntimeException("Purchase order creation failed due to GRN creation error: " + e.getMessage(), e);
            }
        } else {
            log.info("Purchase Order ID: {} is pending manager approval (exceeds max stock level). Skipping automatic GRN creation.",
                    savedPurchaseOrder.getPoId());
            
            // Send notification to Manager for approval
            notificationService.sendNotification(
                "Purchase Order Awaiting Approval",
                "New PO #" + savedPurchaseOrder.getPoId() + " from " + supplier.getName() + " needs your approval.",
                "warning",
                "2" // Manager
            );
        }

        // Send notification to Storekeeper (Creation confirmation)
        notificationService.sendNotification(
            "Purchase Order Created",
            "PO #" + savedPurchaseOrder.getPoId() + " has been created with status: " + status,
            "success",
            "3" // Storekeeper
        );

        // Step 8: Build response
        return buildPurchaseOrderResponse(savedPurchaseOrder, supplier, savedItems, itemsData, totalEstimatedCost);
    }

    @Override
    public CreatePurchaseOrderResponseDto approvePurchaseOrder(Long poId) {
        log.info("Manager approving Purchase Order ID: {}", poId);

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found with ID: " + poId));

        if (!"PENDING".equalsIgnoreCase(purchaseOrder.getStatus()) && !"Pending Manager Approval".equalsIgnoreCase(purchaseOrder.getStatus())) {
            throw new IllegalStateException("Purchase order is not in PENDING state. Current status: " + purchaseOrder.getStatus());
        }

        // Update status
        purchaseOrder.setStatus("APPROVED");
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        // Call GRN service
        try {
            grnService.createGrnForPurchaseOrder(savedPurchaseOrder);
            log.info("Successfully created GRN for newly approved purchase order ID: {}", savedPurchaseOrder.getPoId());
        } catch (GrnCreationException e) {
            log.error("Failed to create GRN for newly approved purchase order ID: {}. Error: {}",
                    savedPurchaseOrder.getPoId(), e.getMessage());
            throw new RuntimeException("Purchase order approved, but GRN creation failed: " + e.getMessage(), e);
        }

        // Notify Storekeeper about approval
        notificationService.sendNotification(
            "Purchase Order Approved",
            "Your PO #" + savedPurchaseOrder.getPoId() + " has been approved by the manager.",
            "success",
            "3" // Storekeeper
        );

        // Build items list response formatting
        Supplier supplier = getSupplierById(savedPurchaseOrder.getSupplierId());
        List<PurchaseOrderItem> items = savedPurchaseOrder.getPurchaseOrderItems();
        
        List<PurchaseOrderItemData> itemsData = items.stream().map(item -> {
            RawMaterial rawMaterial = rawMaterialRepository.findById((long) item.getRawMaterialId())
                    .orElseThrow(() -> new RawMaterialNotFoundException((long) item.getRawMaterialId()));
            return PurchaseOrderItemData.builder()
                    .rawMaterial(rawMaterial)
                    .requiredQty((double) item.getRequiredQty())
                    .actualCost(item.getActualCost())
                    .estimatedCost(item.getEstimatedCost())
                    .totalActualCost(item.getActualCost().multiply(BigDecimal.valueOf(item.getRequiredQty())))
                    .totalEstimatedCost(item.getEstimatedCost().multiply(BigDecimal.valueOf(item.getRequiredQty())))
                    .unitOfMeasure(item.getUnitOfMeasure())
                    .build();
        }).toList();

        BigDecimal totalEstimatedCost = calculateTotalEstimatedCost(itemsData);

        return buildPurchaseOrderResponse(savedPurchaseOrder, supplier, items, itemsData, totalEstimatedCost);
    }

    /**
     * Validates the purchase order request.
     */
    private void validatePurchaseOrderRequest(CreatePurchaseOrderRequestDto requestDto) {
        // Validate delivery date
        if (requestDto.getEstimatedDeliveryDate().isBefore(LocalDate.now())
                || requestDto.getEstimatedDeliveryDate().isEqual(LocalDate.now())) {
            throw new InvalidDeliveryDateException(requestDto.getEstimatedDeliveryDate());
        }

        // Basic validation - items should not be empty (handled by validation annotations)
        if (requestDto.getItems() == null || requestDto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Purchase order must contain at least one item");
        }
    }

    /**
     * Gets supplier by ID, throws exception if not found.
     */
    private Supplier getSupplierById(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
    }

    /**
     * Validates supplier-material relationships and calculates item costs.
     */
    private List<PurchaseOrderItemData> validateAndCalculateItems(CreatePurchaseOrderRequestDto requestDto, Supplier supplier) {
        List<PurchaseOrderItemData> itemsData = new ArrayList<>();

        for (PurchaseOrderItemRequestDto itemRequest : requestDto.getItems()) {
            // Validate raw material exists
            RawMaterial rawMaterial = rawMaterialRepository.findById(itemRequest.getRawMaterialId())
                    .orElseThrow(() -> new RawMaterialNotFoundException(itemRequest.getRawMaterialId()));

            // Validate supplier can supply this material (Relaxed for fallback purposes)
            Optional<RawMaterialSupplier> supplierMaterialOpt = rawMaterialSupplierRepository
                    .findByRawMaterialIdAndSupplierId(itemRequest.getRawMaterialId(), supplier.getSupplierId());

            RawMaterialSupplier supplierMaterial = supplierMaterialOpt.orElse(null);

            // Calculate costs
            Double negotiatedUnitCost;
            if (supplierMaterial != null && supplierMaterial.getNegotiatedUnitCost() != null) {
                negotiatedUnitCost = supplierMaterial.getNegotiatedUnitCost();
            } else {
                log.warn("Supplier mapping or negotiated unit cost is missing for supplier ID: {} and material ID: {}. Falling back to material unit cost.",
                        supplier.getSupplierId(), rawMaterial.getId());
                negotiatedUnitCost = rawMaterial.getUnitCost() != null ? rawMaterial.getUnitCost() : 0.0;
            }
            
            BigDecimal estimatedCost = BigDecimal.valueOf(negotiatedUnitCost);
            BigDecimal actualCost = itemRequest.getActualCost();
            BigDecimal totalEstimatedCost = estimatedCost.multiply(BigDecimal.valueOf(itemRequest.getRequiredQty()));
            BigDecimal totalActualCost = actualCost.multiply(BigDecimal.valueOf(itemRequest.getRequiredQty()));

            PurchaseOrderItemData itemData = PurchaseOrderItemData.builder()
                    .rawMaterial(rawMaterial)
                    .supplierMaterial(supplierMaterial)
                    .requiredQty(itemRequest.getRequiredQty())
                    .unitOfMeasure(itemRequest.getUnitOfMeasure())
                    .actualCost(actualCost)
                    .estimatedCost(estimatedCost)
                    .totalActualCost(totalActualCost)
                    .totalEstimatedCost(totalEstimatedCost)
                    .build();

            itemsData.add(itemData);
        }

        return itemsData;
    }

    /**
     * Calculates total actual cost for all items.
     */
    private BigDecimal calculateTotalActualCost(List<PurchaseOrderItemData> itemsData) {
        return itemsData.stream()
                .map(PurchaseOrderItemData::getTotalActualCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total estimated cost for all items.
     */
    private BigDecimal calculateTotalEstimatedCost(List<PurchaseOrderItemData> itemsData) {
        return itemsData.stream()
                .map(PurchaseOrderItemData::getTotalEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Creates the purchase order entity.
     */
    private PurchaseOrder createPurchaseOrderEntity(CreatePurchaseOrderRequestDto requestDto,
            Supplier supplier,
            BigDecimal totalCost,
            int numberOfItems,
            String status) {
        return PurchaseOrder.builder()
                .supplierId(supplier.getSupplierId())
                .totalCost(totalCost)
                .numberOfItems(numberOfItems)
                .estimatedDeliveryDate(requestDto.getEstimatedDeliveryDate())
                .status(status)
                .build();
    }

    /**
     * Creates and saves purchase order items.
     */
    private List<PurchaseOrderItem> createAndSavePurchaseOrderItems(PurchaseOrder purchaseOrder,
            List<PurchaseOrderItemData> itemsData) {
        List<PurchaseOrderItem> purchaseOrderItems = new ArrayList<>();

        for (PurchaseOrderItemData itemData : itemsData) {
            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(purchaseOrder)
                    .rawMaterialId(itemData.getRawMaterial().getId().intValue())
                    .requiredQty(itemData.getRequiredQty().intValue())
                    .receivedQty(null)
                    .actualCost(itemData.getActualCost())
                    .estimatedCost(itemData.getEstimatedCost())
                    .unitOfMeasure(itemData.getUnitOfMeasure())
                    .build();

            purchaseOrderItems.add(item);
        }

        return purchaseOrderItemRepository.saveAll(purchaseOrderItems);
    }

    /**
     * Builds the response DTO.
     */
    private CreatePurchaseOrderResponseDto buildPurchaseOrderResponse(PurchaseOrder purchaseOrder,
            Supplier supplier,
            List<PurchaseOrderItem> savedItems,
            List<PurchaseOrderItemData> itemsData,
            BigDecimal totalEstimatedCost) {

        // Create item response DTOs
        List<PurchaseOrderItemResponseDto> itemResponses = new ArrayList<>();
        for (int i = 0; i < savedItems.size(); i++) {
            PurchaseOrderItem savedItem = savedItems.get(i);
            PurchaseOrderItemData itemData = itemsData.get(i);

            PurchaseOrderItemResponseDto itemResponse = PurchaseOrderItemResponseDto.builder()
                    .poiId(savedItem.getPoiId())
                    .rawMaterialId(itemData.getRawMaterial().getId())
                    .rawMaterialName(itemData.getRawMaterial().getMaterialName())
                    .requiredQty(itemData.getRequiredQty())
                    .unitOfMeasure(itemData.getUnitOfMeasure())
                    .actualCost(itemData.getActualCost())
                    .estimatedCost(itemData.getEstimatedCost())
                    .totalActualCost(itemData.getTotalActualCost())
                    .totalEstimatedCost(itemData.getTotalEstimatedCost())
                    .build();

            itemResponses.add(itemResponse);
        }

        // Calculate cost variance
        BigDecimal costVariance = purchaseOrder.getTotalCost().subtract(totalEstimatedCost);
        Double costVariancePercentage = totalEstimatedCost.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : costVariance.divide(totalEstimatedCost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();

        return CreatePurchaseOrderResponseDto.builder()
                .poId(purchaseOrder.getPoId())
                .supplierId(supplier.getSupplierId())
                .supplierName(supplier.getName())
                .totalCost(purchaseOrder.getTotalCost())
                .numberOfItems(purchaseOrder.getNumberOfItems())
                .estimatedDeliveryDate(purchaseOrder.getEstimatedDeliveryDate())
                .status(purchaseOrder.getStatus())
                .items(itemResponses)
                .totalEstimatedCost(totalEstimatedCost)
                .costVariance(costVariance)
                .costVariancePercentage(costVariancePercentage)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Internal class to hold purchase order item data during processing.
     */
    @lombok.Data
    @lombok.Builder
    private static class PurchaseOrderItemData {

        private RawMaterial rawMaterial;
        private RawMaterialSupplier supplierMaterial;
        private Double requiredQty;
        private String unitOfMeasure;
        private BigDecimal actualCost;
        private BigDecimal estimatedCost;
        private BigDecimal totalActualCost;
        private BigDecimal totalEstimatedCost;
    }
}
