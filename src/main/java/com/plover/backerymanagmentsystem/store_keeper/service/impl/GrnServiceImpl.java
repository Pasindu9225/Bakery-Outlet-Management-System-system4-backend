package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.store_keeper.dto.GetAllGrnsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.PartialReceiptResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.GrnCreationException;
import com.plover.backerymanagmentsystem.store_keeper.exception.GrnNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.GrnReceiveException;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnItem;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.GrnService;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialBatchService;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of GrnService for managing Goods Receipt Notes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GrnServiceImpl implements GrnService {

    private final GrnRepository grnRepository;
    private final GrnItemRepository grnItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final RawMaterialBatchService rawMaterialBatchService;
    private final RawMaterialRepository rawMaterialRepository;

    @Override
    public Grn createGrnForPurchaseOrder(PurchaseOrder purchaseOrder) {
        try {
            log.info("Creating GRN for purchase order ID: {}", purchaseOrder.getPoId());

            // Check if GRN already exists for this purchase order
            if (grnRepository.existsByPoId(purchaseOrder.getPoId())) {
                log.warn("GRN already exists for purchase order ID: {}", purchaseOrder.getPoId());
                throw new GrnCreationException("GRN already exists for purchase order ID: " + purchaseOrder.getPoId());
            }

            // Create GRN entity
            Grn grn = Grn.builder()
                    .poId(purchaseOrder.getPoId())
                    .supplierId(purchaseOrder.getSupplierId())
                    .total(purchaseOrder.getTotalCost())
                    .grnStatus(GrnStatus.PENDING)
                    .isReceived(false)
                    .build();

            // Save GRN
            Grn savedGrn = grnRepository.save(grn);
            log.info("Created GRN with ID: {} for PO ID: {}", savedGrn.getGrnId(), purchaseOrder.getPoId());

            // Create GRN items for each purchase order item
            List<GrnItem> grnItems = createGrnItems(savedGrn, purchaseOrder.getPurchaseOrderItems());

            // Save GRN items
            List<GrnItem> savedGrnItems = grnItemRepository.saveAll(grnItems);
            log.info("Created {} GRN items for GRN ID: {}", savedGrnItems.size(), savedGrn.getGrnId());

            // Set the items in the GRN for return
            savedGrn.setGrnItems(savedGrnItems);

            return savedGrn;

        } catch (GrnCreationException e) {
            // Re-throw GRN-specific exceptions
            throw e;
        } catch (RuntimeException e) {
            log.error("Runtime exception while creating GRN for purchase order ID: {}", purchaseOrder.getPoId(), e);
            throw new GrnCreationException("Failed to create GRN for purchase order: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected exception while creating GRN for purchase order ID: {}", purchaseOrder.getPoId(), e);
            throw new GrnCreationException("Failed to create GRN for purchase order: " + e.getMessage(), e);
        }
    }

    @Override
    public GrnReceiveResponseDto receiveGoods(Long grnId, GrnReceiveRequestDto request) {
        try {
            log.info("Processing goods receipt for GRN ID: {} with status: {}", grnId, request.getGrnStatus());

            // Find the GRN with a pessimistic write lock to prevent race conditions / duplicate receipt
            Grn grn = grnRepository.findByGrnIdForUpdate(grnId)
                    .orElseThrow(() -> new GrnNotFoundException(grnId));

            // Check if GRN is already received/processed
            if (grn.getIsReceived() != null && grn.getIsReceived()) {
                log.warn("GRN ID: {} has already been received and processed.", grnId);
                throw new GrnReceiveException("This GRN has already been submitted and received. Duplicate submissions are not allowed.");
            }

            // Get all GRN items
            List<GrnItem> grnItems = grnItemRepository.findByGrnIdWithRawMaterial(grnId);
            if (grnItems.isEmpty()) {
                throw new GrnReceiveException("No GRN items found for GRN ID: " + grnId);
            }

            // Validate matching invoice/actual quantity & price and auto-assign all batch numbers
            java.util.Set<String> processedKeys = new java.util.HashSet<>();
            for (GrnReceiveRequestDto.GrnReceiveItemDto itemRequest : request.getItems()) {
                BigDecimal actQty = itemRequest.getActualQuantity() != null ? itemRequest.getActualQuantity() : itemRequest.getReceivedQuantity();
                if (actQty == null) actQty = BigDecimal.ZERO;
                itemRequest.setReceivedQuantity(actQty);

                if (actQty.compareTo(BigDecimal.ZERO) > 0) {
                    GrnItem grnItem = grnItems.stream()
                            .filter(item -> item.getGrnItemId().equals(itemRequest.getGrnItemId()))
                            .findFirst()
                            .orElse(null);
                    
                    if (grnItem != null) {
                        RawMaterial baseMaterial = rawMaterialRepository.findById(grnItem.getRawMaterialId())
                                .orElseThrow(() -> new GrnReceiveException("Raw material not found with ID: " + grnItem.getRawMaterialId()));

                        BigDecimal invQty = itemRequest.getInvoiceQuantity() != null ? itemRequest.getInvoiceQuantity() : actQty;
                        BigDecimal invPrice = itemRequest.getInvoicePrice() != null ? itemRequest.getInvoicePrice() : 
                                (itemRequest.getActualPrice() != null ? itemRequest.getActualPrice() : grnItem.getPricePerUnit());
                        BigDecimal actPrice = itemRequest.getActualPrice() != null ? itemRequest.getActualPrice() : invPrice;

                        itemRequest.setInvoiceQuantity(invQty);
                        itemRequest.setActualQuantity(actQty);
                        itemRequest.setInvoicePrice(invPrice);
                        itemRequest.setActualPrice(actPrice);

                        // Strict matching check
                        if (invQty.compareTo(actQty) != 0) {
                            throw new GrnReceiveException("Invoice Quantity (" + invQty + ") does not match Actual Quantity (" + 
                                    actQty + ") for material '" + baseMaterial.getMaterialName() + "'. Quantities must match to complete GRN.");
                        }

                        if (invPrice.compareTo(actPrice) != 0) {
                            throw new GrnReceiveException("Invoice Price (" + invPrice + ") does not match Actual Price (" + 
                                    actPrice + ") for material '" + baseMaterial.getMaterialName() + "'. Prices must match to complete GRN.");
                        }

                        String batchNo = resolveOrGenerateBatchNo(itemRequest.getBatchNo(), grnItem.getRawMaterialId(), processedKeys);
                        itemRequest.setBatchNo(batchNo);
                        
                        String uniqueKey = baseMaterial.getMaterialCode() + "-" + batchNo;
                        if (processedKeys.contains(uniqueKey)) {
                            throw new GrnReceiveException("Duplicate batch number '" + batchNo + 
                                    "' entered in this request for raw material '" + baseMaterial.getMaterialName() + "'.");
                        }
                        processedKeys.add(uniqueKey);
                    }
                }
            }

            // Process each item in the request
            List<GrnReceiveResponseDto.GrnItemUpdateDto> updatedItems = new ArrayList<>();
            List<String> batchCreationResults = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            boolean anyPartial = false;
            BigDecimal calculatedGrnTotal = BigDecimal.ZERO;

            for (GrnReceiveRequestDto.GrnReceiveItemDto itemRequest : request.getItems()) {
                // Find the corresponding GRN item
                GrnItem grnItem = grnItems.stream()
                        .filter(item -> item.getGrnItemId().equals(itemRequest.getGrnItemId()))
                        .findFirst()
                        .orElse(null);

                if (grnItem != null) {
                    BigDecimal actQty = itemRequest.getActualQuantity() != null ? itemRequest.getActualQuantity() : itemRequest.getReceivedQuantity();
                    if (actQty == null) actQty = BigDecimal.ZERO;
                    BigDecimal invPrice = itemRequest.getInvoicePrice() != null ? itemRequest.getInvoicePrice() : 
                            (itemRequest.getActualPrice() != null ? itemRequest.getActualPrice() : grnItem.getPricePerUnit());

                    // Sum previous and new received quantities (partial handling)
                    BigDecimal previousQuantity = grnItem.getReceivedQuantity() != null
                            ? grnItem.getReceivedQuantity() : BigDecimal.ZERO;
                    BigDecimal newTotalReceived = previousQuantity.add(actQty);
                    grnItem.setReceivedQuantity(newTotalReceived);
                    grnItem.setInvoiceQuantity(itemRequest.getInvoiceQuantity());
                    grnItem.setInvoicePrice(invPrice);
                    grnItem.setPricePerUnit(invPrice); // Use invoice price for line price & finance
                    grnItem.setBatchNo(itemRequest.getBatchNo());
                    grnItem.setExpireDate(itemRequest.getExpireDate());

                    BigDecimal itemLineTotal = invPrice.multiply(actQty);
                    calculatedGrnTotal = calculatedGrnTotal.add(itemLineTotal);

                    // Create update info
                    GrnReceiveResponseDto.GrnItemUpdateDto updateDto = GrnReceiveResponseDto.GrnItemUpdateDto.builder()
                            .grnItemId(grnItem.getGrnItemId())
                            .rawMaterialId(grnItem.getRawMaterialId())
                            .rawMaterialName(grnItem.getRawMaterial() != null ? grnItem.getRawMaterial().getMaterialName() : "Unknown")
                            .previousReceivedQuantity(previousQuantity)
                            .newReceivedQuantity(newTotalReceived)
                            .uom(grnItem.getUom())
                            .build();
                    updatedItems.add(updateDto);

                    if (actQty.compareTo(BigDecimal.ZERO) > 0) {
                        // Create new batch using actualQuantity for stock and invoicePrice for ledger/costing
                        try {
                            String supplierName = grn.getSupplier() != null ? grn.getSupplier().getName() : "Unknown";
                            Long newBatchId = rawMaterialBatchService.createNewBatch(
                                    grnItem.getRawMaterialId(), 
                                    itemRequest.getBatchNo(), 
                                    actQty, // Stock update uses actual quantity
                                    supplierName,
                                    invPrice, // Ledger / Finance uses invoice price
                                    itemRequest.getExpireDate()
                            );
                            batchCreationResults.add("Created batch ID: " + newBatchId + " for material: " + 
                                    (grnItem.getRawMaterial() != null ? grnItem.getRawMaterial().getMaterialName() : "Unknown"));
                        } catch (Exception e) {
                            String error = "Failed to create batch for material ID " + grnItem.getRawMaterialId() + ": " + e.getMessage();
                            log.error(error, e);
                            throw new GrnReceiveException("Failed to create batch for material '" + 
                                    (grnItem.getRawMaterial() != null ? grnItem.getRawMaterial().getMaterialName() : grnItem.getRawMaterialId()) + "': " + e.getMessage(), e);
                        }
                    }
                }
            }

            // Save updated GRN items
            grnItemRepository.saveAll(grnItems);

            // Update GRN total for Finance / Supplier Ledger using invoice price * actual quantity
            if (calculatedGrnTotal.compareTo(BigDecimal.ZERO) > 0) {
                grn.setTotal(calculatedGrnTotal);
            }

            // Decide GRN status: PARTIAL if any item not fully received now
            if (request.getGrnStatus() == com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus.PARTIAL || anyPartial) {
                grn.setGrnStatus(com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus.PARTIAL);
                grn.setIsReceived(false);
            } else {
                grn.setGrnStatus(request.getGrnStatus());
                grn.setIsReceived(true);
            }
            grn.setReceivedDate(java.time.LocalDateTime.now());
            grn.setInvoiceNumber(request.getInvoiceNumber());
            if (request.getStorekeeperSignature() != null) {
                grn.setStorekeeperSignature(request.getStorekeeperSignature());
            }
            grnRepository.save(grn);

            log.info("Successfully processed goods receipt for GRN ID: {}", grnId);

            // Build response
            return GrnReceiveResponseDto.builder()
                    .grnId(grn.getGrnId())
                    .poId(grn.getPoId())
                    .supplierId(grn.getSupplierId())
                    .grnStatus(grn.getGrnStatus())
                    .isReceived(grn.getIsReceived())
                    .receivedDate(grn.getReceivedDate())
                    .invoiceNumber(grn.getInvoiceNumber())
                    .total(grn.getTotal())
                    .updatedItems(updatedItems)
                    .stockUpdates(new ArrayList<>()) // No stock updates since we're creating batches
                    .batchCreationResults(batchCreationResults) // Use new field for batch creation results
                    .errors(errors)
                    .success(errors.isEmpty())
                    .message(errors.isEmpty()
                            ? "Goods received successfully and new batches created"
                            : "Goods received with some batch creation errors")
                    .build();

        } catch (GrnNotFoundException | GrnReceiveException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process goods receipt for GRN ID: {}", grnId, e);
            throw new GrnReceiveException("Failed to process goods receipt: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Grn findByPurchaseOrderId(Long poId) {
        return grnRepository.findByPoId(poId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grn> findBySupplier(Long supplierId) {
        return grnRepository.findBySupplierId(supplierId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsForPurchaseOrder(Long poId) {
        return grnRepository.existsByPoId(poId);
    }

    /**
     * Creates GRN items from purchase order items.
     *
     * @param grn the saved GRN
     * @param purchaseOrderItems the purchase order items
     * @return list of GRN items
     */
    private List<GrnItem> createGrnItems(Grn grn, List<PurchaseOrderItem> purchaseOrderItems) {
        List<GrnItem> grnItems = new ArrayList<>();

        for (PurchaseOrderItem poItem : purchaseOrderItems) {
            GrnItem grnItem = GrnItem.builder()
                    .grnId(grn.getGrnId())
                    .rawMaterialId(poItem.getRawMaterialId().longValue())
                    .uom(poItem.getUnitOfMeasure())
                    .pricePerUnit(poItem.getActualCost() != null ? poItem.getActualCost() : poItem.getEstimatedCost()) // Prioritize actual cost
                    .receivedQuantity(java.math.BigDecimal.ZERO) // Initially 0, will be updated when goods are received
                    .build();

            grnItems.add(grnItem);
            log.debug("Created GRN item for raw material ID: {} in GRN ID: {}",
                    poItem.getRawMaterialId(), grn.getGrnId());
        }

        return grnItems;
    }

    @Override
    @Transactional(readOnly = true)
    public GetAllGrnsResponseDto getAllGrns() {
        log.info("Retrieving all GRNs with item counts");

        List<Object[]> grnData = grnRepository.findAllGrnsWithItemCounts();

        List<GetAllGrnsResponseDto.GrnSummaryDto> grnSummaries = new ArrayList<>();

        for (Object[] row : grnData) {
            // Fetch material names for this GRN
            List<GrnItem> items = grnItemRepository.findByGrnIdWithRawMaterial((Long) row[0]);
            String materialNames = items.stream()
                    .map(item -> {
                        if (item.getRawMaterial() == null) return "";
                        String matName = item.getRawMaterial().getMaterialName();
                        String brand = item.getRawMaterial().getBrandName();
                        if (brand != null && !brand.isEmpty() && !brand.equalsIgnoreCase("default") && !brand.equalsIgnoreCase("no brand") && !brand.equalsIgnoreCase("unbranded")) {
                            return matName + " (" + brand + ")";
                        }
                        return matName;
                    })
                    .filter(name -> !name.isEmpty())
                    .collect(java.util.stream.Collectors.joining(", "));

            GetAllGrnsResponseDto.GrnSummaryDto grnSummary = GetAllGrnsResponseDto.GrnSummaryDto.builder()
                    .grnId((Long) row[0])
                    .receivedDate((java.time.LocalDateTime) row[1])
                    .poId((Long) row[2])
                    .supplierId((Long) row[3])
                    .supplierName((String) row[4])
                    .total((java.math.BigDecimal) row[5])
                    .grnStatus((GrnStatus) row[6])
                    .isReceived((Boolean) row[7])
                    .invoiceNumber((String) row[8])
                    .numberOfItems(((Number) row[9]).intValue())
                    .poReference(String.valueOf(row[10]))
                    .materialNames(materialNames)
                    .estimatedDeliveryDate((java.time.LocalDate) row[11])
                    .createdAt((java.time.LocalDateTime) row[12])
                    .storekeeperSignature((String) row[13])
                    .build();

            grnSummaries.add(grnSummary);
        }

        log.info("Retrieved {} GRNs with their item counts", grnSummaries.size());

        return GetAllGrnsResponseDto.builder()
                .grns(grnSummaries)
                .totalCount(grnSummaries.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PartialReceiptResponseDto getPartialReceiptSummary() {
        log.info("Compiling partial receipt summary for purchase orders");

        List<Grn> grnsWithItems = grnRepository.findAllWithItemsAndSupplier();
        if (grnsWithItems.isEmpty()) {
            log.info("No GRNs with items found; returning empty partial receipt summary");
            return PartialReceiptResponseDto.builder()
                    .totalCount(0)
                    .purchaseOrders(Collections.emptyList())
                    .build();
        }

        Map<Long, List<Grn>> grnsByPo = grnsWithItems.stream()
                .filter(grn -> grn.getPoId() != null)
                .collect(Collectors.groupingBy(Grn::getPoId, LinkedHashMap::new, Collectors.toList()));

        if (grnsByPo.isEmpty()) {
            log.warn("GRNs were found but none were associated with a purchase order; returning empty summary");
            return PartialReceiptResponseDto.builder()
                    .totalCount(0)
                    .purchaseOrders(Collections.emptyList())
                    .build();
        }

        Set<Long> poIds = grnsByPo.keySet();
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByPoIdIn(poIds);
        Map<Long, PurchaseOrder> poMap = purchaseOrders.stream()
                .collect(Collectors.toMap(PurchaseOrder::getPoId, po -> po));

        List<PartialReceiptResponseDto.PurchaseOrderPartialDto> purchaseOrderDtos = new ArrayList<>();

        for (Long poId : grnsByPo.keySet()) {
            PurchaseOrder purchaseOrder = poMap.get(poId);
            if (purchaseOrder == null) {
                log.warn("Skipping partial receipt aggregation for PO ID {} because the purchase order could not be loaded", poId);
                continue;
            }

            List<Grn> orderGrns = grnsByPo.getOrDefault(poId, Collections.emptyList());
            AggregatedPurchaseOrder aggregated = aggregatePurchaseOrder(purchaseOrder, orderGrns);

            List<PartialReceiptResponseDto.PendingItem> pendingItems = aggregated.getPendingItems();
            boolean hasPendingBalances = pendingItems.stream()
                    .anyMatch(item -> item.getBalanceQty().compareTo(BigDecimal.ZERO) > 0);

            String overallStatus = deriveOverallStatus(aggregated, hasPendingBalances);

            PartialReceiptResponseDto.PurchaseOrderPartialDto dto = PartialReceiptResponseDto.PurchaseOrderPartialDto.builder()
                    .poId(purchaseOrder.getPoId())
                    .poReference(String.valueOf(purchaseOrder.getPoId()))
                    .supplier(buildSupplierSummary(orderGrns))
                    .summary(PartialReceiptResponseDto.PurchaseOrderSummary.builder()
                            .estimatedDeliveryDate(purchaseOrder.getEstimatedDeliveryDate())
                            .totalCost(purchaseOrder.getTotalCost())
                            .numberOfItems(purchaseOrder.getNumberOfItems())
                            .overallGrnStatus(overallStatus)
                            .build())
                    .grns(aggregated.getGrnSummaries())
                    .pendingItems(pendingItems)
                    .build();

            purchaseOrderDtos.add(dto);
        }

        purchaseOrderDtos.sort((left, right) -> left.getPoId().compareTo(right.getPoId()));

        return PartialReceiptResponseDto.builder()
                .totalCount(purchaseOrderDtos.size())
                .purchaseOrders(purchaseOrderDtos)
                .build();
    }

    private PartialReceiptResponseDto.SupplierSummary buildSupplierSummary(List<Grn> grns) {
        if (grns.isEmpty()) {
            return PartialReceiptResponseDto.SupplierSummary.builder().build();
        }

        Supplier supplier = grns.stream()
                .map(Grn::getSupplier)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Long supplierId = grns.stream()
                .map(Grn::getSupplierId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return PartialReceiptResponseDto.SupplierSummary.builder()
                .supplierId(supplierId)
                .supplierName(supplier != null ? supplier.getName() : null)
                .build();
    }

    private String deriveOverallStatus(AggregatedPurchaseOrder aggregated, boolean hasPendingBalances) {
        if (aggregated.getGrnSummaries().isEmpty()) {
            return GrnStatus.PENDING.name();
        }

        if (aggregated.isAllCancelled()) {
            return GrnStatus.CANCELLED.name();
        }

        if (!aggregated.hasAnyReceipt()) {
            return GrnStatus.PENDING.name();
        }

        if (hasPendingBalances) {
            return GrnStatus.PARTIAL.name();
        }

        return GrnStatus.RECEIVED.name();
    }

    private AggregatedPurchaseOrder aggregatePurchaseOrder(PurchaseOrder purchaseOrder, List<Grn> grns) {
        Map<Long, AggregatedMaterial> materialMap = new LinkedHashMap<>();

        for (PurchaseOrderItem poItem : safeList(purchaseOrder.getPurchaseOrderItems())) {
            Long rawMaterialId = poItem.getRawMaterialId() != null
                    ? poItem.getRawMaterialId().longValue()
                    : null;
            if (rawMaterialId == null) {
                continue;
            }

            AggregatedMaterial material = materialMap.computeIfAbsent(rawMaterialId, AggregatedMaterial::new);
            material.addRequiredQuantity(poItem.getRequiredQty());
            material.tryPopulateFromPurchaseOrderItem(poItem);
        }

        List<PartialReceiptResponseDto.GrnSummary> grnSummaries = new ArrayList<>();
        boolean hasAnyReceipt = false;
        boolean allCancelled = !grns.isEmpty();

        for (Grn grn : grns) {
            grnSummaries.add(PartialReceiptResponseDto.GrnSummary.builder()
                    .grnId(grn.getGrnId())
                    .grnStatus(grn.getGrnStatus() != null ? grn.getGrnStatus().name() : null)
                    .receivedDate(grn.getReceivedDate())
                    .total(grn.getTotal())
                    .build());

            if (grn.getGrnStatus() != GrnStatus.CANCELLED) {
                allCancelled = false;
            }

            for (GrnItem grnItem : safeList(grn.getGrnItems())) {
                Long rawMaterialId = grnItem.getRawMaterialId();
                if (rawMaterialId == null) {
                    continue;
                }

                AggregatedMaterial material = materialMap.computeIfAbsent(rawMaterialId, AggregatedMaterial::new);
                BigDecimal receivedQty = scaleQuantity(grnItem.getReceivedQuantity());
                if (receivedQty.compareTo(BigDecimal.ZERO) > 0) {
                    hasAnyReceipt = true;
                }

                material.addReceivedQuantity(receivedQty, grn.getGrnId());
                material.tryPopulateFromGrnItem(grnItem);
            }
        }

        if (!hasAnyReceipt) {
            hasAnyReceipt = materialMap.values().stream()
                    .anyMatch(material -> material.getReceivedQuantity().compareTo(BigDecimal.ZERO) > 0);
        }

        grnSummaries.sort((left, right) -> left.getGrnId().compareTo(right.getGrnId()));

        return new AggregatedPurchaseOrder(grnSummaries, materialMap, hasAnyReceipt, allCancelled);
    }

    private static <T> List<T> safeList(List<T> items) {
        return items == null ? Collections.emptyList() : items;
    }

    private static BigDecimal scaleQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP)
                : value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleQuantity(Integer value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scalePrice(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private static final int QUANTITY_SCALE = 3;
    private static final int PRICE_SCALE = 2;

    private static class AggregatedPurchaseOrder {

        private final List<PartialReceiptResponseDto.GrnSummary> grnSummaries;
        private final Map<Long, AggregatedMaterial> materials;
        private final boolean hasAnyReceipt;
        private final boolean allCancelled;

        AggregatedPurchaseOrder(List<PartialReceiptResponseDto.GrnSummary> grnSummaries,
                Map<Long, AggregatedMaterial> materials,
                boolean hasAnyReceipt,
                boolean allCancelled) {
            this.grnSummaries = grnSummaries;
            this.materials = materials;
            this.hasAnyReceipt = hasAnyReceipt;
            this.allCancelled = allCancelled;
        }

        List<PartialReceiptResponseDto.GrnSummary> getGrnSummaries() {
            return grnSummaries;
        }

        List<PartialReceiptResponseDto.PendingItem> getPendingItems() {
            return materials.values().stream()
                    .map(AggregatedMaterial::toPendingItem)
                    .filter(Objects::nonNull)
                    .sorted((left, right) -> left.getRawMaterialId().compareTo(right.getRawMaterialId()))
                    .collect(Collectors.toList());
        }

        boolean hasAnyReceipt() {
            return hasAnyReceipt;
        }

        boolean isAllCancelled() {
            return allCancelled;
        }
    }

    private static class AggregatedMaterial {

        private final Long rawMaterialId;
        private BigDecimal requiredQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        private BigDecimal receivedQuantity = BigDecimal.ZERO.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        private String rawMaterialName;
        private String unitOfMeasure;
        private BigDecimal unitPrice;
        private final Map<Long, BigDecimal> grnBreakdown = new LinkedHashMap<>();

        AggregatedMaterial(Long rawMaterialId) {
            this.rawMaterialId = rawMaterialId;
        }

        void addRequiredQuantity(Integer qty) {
            requiredQuantity = requiredQuantity.add(scaleQuantity(qty));
        }

        void addReceivedQuantity(BigDecimal qty, Long grnId) {
            receivedQuantity = receivedQuantity.add(qty);
            if (grnId != null) {
                grnBreakdown.merge(grnId, qty, BigDecimal::add);
            }
        }

        void tryPopulateFromPurchaseOrderItem(PurchaseOrderItem poItem) {
            if (poItem.getRawMaterial() != null) {
                if (rawMaterialName == null) {
                    rawMaterialName = poItem.getRawMaterial().getMaterialName();
                }
                if (unitOfMeasure == null) {
                    unitOfMeasure = poItem.getRawMaterial().getUnitOfMeasure();
                }
                if (unitPrice == null) {
                    unitPrice = scalePrice(poItem.getRawMaterial().getUnitCost());
                }
            }

            if (unitOfMeasure == null) {
                unitOfMeasure = poItem.getUnitOfMeasure();
            }
        }

        void tryPopulateFromGrnItem(GrnItem grnItem) {
            if (rawMaterialName == null && grnItem.getRawMaterial() != null) {
                rawMaterialName = grnItem.getRawMaterial().getMaterialName();
            }

            if (unitOfMeasure == null) {
                unitOfMeasure = grnItem.getUom();
            }

            if (unitPrice == null && grnItem.getRawMaterial() != null) {
                unitPrice = scalePrice(grnItem.getRawMaterial().getUnitCost());
            }

            if (unitPrice == null && grnItem.getPricePerUnit() != null) {
                unitPrice = grnItem.getPricePerUnit().setScale(PRICE_SCALE, RoundingMode.HALF_UP);
            }
        }

        BigDecimal getReceivedQuantity() {
            return receivedQuantity;
        }

        PartialReceiptResponseDto.PendingItem toPendingItem() {
            BigDecimal balance = requiredQuantity.subtract(receivedQuantity);
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }

            List<PartialReceiptResponseDto.GrnReceiptBreakdown> breakdownDtos = grnBreakdown.entrySet().stream()
                    .map(entry -> PartialReceiptResponseDto.GrnReceiptBreakdown.builder()
                    .grnId(entry.getKey())
                    .receivedQty(entry.getValue().setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                    .build())
                    .sorted((left, right) -> left.getGrnId().compareTo(right.getGrnId()))
                    .collect(Collectors.toList());

            return PartialReceiptResponseDto.PendingItem.builder()
                    .rawMaterialId(rawMaterialId)
                    .rawMaterialName(rawMaterialName)
                    .unitOfMeasure(unitOfMeasure)
                    .unitPrice(unitPrice)
                    .requiredQty(requiredQuantity)
                    .receivedQty(receivedQuantity)
                    .balanceQty(balance.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP))
                    .grnBreakdown(breakdownDtos)
                    .build();
        }
    }

    private String resolveOrGenerateBatchNo(String providedBatchNo, Long rawMaterialId, java.util.Set<String> processedKeys) {
        if (providedBatchNo != null && !providedBatchNo.trim().isEmpty()) {
            return providedBatchNo.trim();
        }

        String matCode = "RM" + rawMaterialId;
        RawMaterial baseMaterial = rawMaterialRepository.findById(rawMaterialId).orElse(null);
        if (baseMaterial != null && baseMaterial.getMaterialCode() != null && !baseMaterial.getMaterialCode().trim().isEmpty()) {
            matCode = baseMaterial.getMaterialCode();
        }

        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "BAT-" + dateStr + "-" + matCode + "-";

        int seq = 1;
        String candidate = prefix + String.format("%02d", seq);
        while (processedKeys.contains(matCode + "-" + candidate) ||
               rawMaterialRepository.findByMaterialCodeAndBatchNo(matCode, candidate).isPresent()) {
            seq++;
            candidate = prefix + String.format("%02d", seq);
        }
        return candidate;
    }
}
