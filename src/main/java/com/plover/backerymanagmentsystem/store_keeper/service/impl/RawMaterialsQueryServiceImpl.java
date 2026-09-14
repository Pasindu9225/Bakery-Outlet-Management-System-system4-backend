package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.SupplierRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.SupplierRawMaterialsResponseDto.SupplierRawMaterialDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialBatchDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialWithBatchesDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.AllRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.AllRawMaterialsResponseDto.RawMaterialDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.PurchaseOrderRawMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.PurchaseOrderRawMaterialsResponseDto.PurchaseOrderRawMaterialDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.PurchaseOrderNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialsNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.SupplierNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialSupplier;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnItem;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialSupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialsQueryService;
import com.plover.backerymanagmentsystem.store_keeper.dto.AggregatedStockResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of RawMaterialsQueryService for managing raw material query
 * operations. Handles retrieval of raw material information with proper
 * business logic and error handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RawMaterialsQueryServiceImpl implements RawMaterialsQueryService {

    private final RawMaterialRepository rawMaterialRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final GrnRepository grnRepository;
    private final GrnItemRepository grnItemRepository;
    private final RawMaterialSupplierRepository rawMaterialSupplierRepository;

    @Override
    public List<AggregatedStockResponseDto> getAggregatedStock() {
        log.info("Retrieving aggregated raw material stock grouped by generic material and brands");
        List<Object[]> results = rawMaterialRepository.findAggregatedStock();

        // Group results by Generic Material ID
        java.util.Map<Long, AggregatedStockResponseDto> grouped = new java.util.LinkedHashMap<>();

        for (Object[] row : results) {
            Long gmId = (row[0] != null ? (Long) row[0] : 0L);
            String gmName = (row[1] != null ? (String) row[1] : "Uncategorized");
            String category = (row[2] != null ? (String) row[2] : "GENERAL");
            String unit = (row[3] != null ? (String) row[3] : "units");
            String brandName = (row[4] != null ? (String) row[4] : "Default");
            Double brandStock = (row[5] != null ? (Double) row[5] : 0.0);

            AggregatedStockResponseDto dto = grouped.computeIfAbsent(gmId, id -> AggregatedStockResponseDto.builder()
                    .id(id)
                    .name(gmName)
                    .category(category)
                    .unitOfMeasure(unit)
                    .totalStock(0.0)
                    .brands(new java.util.ArrayList<>())
                    .build());

            dto.setTotalStock(dto.getTotalStock() + brandStock);
            dto.getBrands().add(AggregatedStockResponseDto.BrandStockSummary.builder()
                    .brandName(brandName)
                    .stock(brandStock)
                    .build());
        }

        return new java.util.ArrayList<>(grouped.values());
    }

    @Override
    public List<RawMaterialWithBatchesDto> getAllRawMaterialsWithDetails() {
        List<RawMaterial> materials = rawMaterialRepository.findAll();

        // Group by material code to aggregate batches under same material
        Map<String, List<RawMaterial>> byCode = materials.stream()
                .collect(Collectors.groupingBy(RawMaterial::getMaterialCode));

        return byCode.entrySet().stream()
                .map(entry -> buildMaterialWithBatches(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RawMaterialWithBatchesDto::getName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private RawMaterialWithBatchesDto buildMaterialWithBatches(String materialCode, List<RawMaterial> batches) {
        if (batches == null || batches.isEmpty()) {
            return RawMaterialWithBatchesDto.builder()
                    .code(materialCode)
                    .batches(Collections.emptyList())
                    .totalQuantity(0.0)
                    .build();
        }

        RawMaterial base = batches.get(0);

        List<RawMaterialBatchDto> batchDtos = batches.stream()
                .sorted((r1, r2) -> {
                    String b1 = r1.getBatchNo();
                    String b2 = r2.getBatchNo();
                    if (b1 == null && b2 == null) return 0;
                    if (b1 == null) return -1;
                    if (b2 == null) return 1;
                    try {
                        return Integer.compare(Integer.parseInt(b1), Integer.parseInt(b2));
                    } catch (NumberFormatException e) {
                        return b1.compareTo(b2);
                    }
                })
                .map(r -> {
                    double initial = r.getInitialQuantity() != null ? r.getInitialQuantity() : (r.getCurrentStock() != null ? r.getCurrentStock() : 0.0);
                    double current = r.getCurrentStock() != null ? r.getCurrentStock() : 0.0;
                    double issued = Math.max(0, initial - current);
                    
                    return RawMaterialBatchDto.builder()
                        .batchNo(r.getBatchNo())
                        .supplier(r.getSupplierName())
                        .purchasePrice(r.getUnitCost())
                        .quantity(current)
                        .receivedQuantity(initial)
                        .issuedQuantity(issued)
                        .balance(current)
                        .expiryDate(r.getExpireDate() != null ? r.getExpireDate().toString() : null)
                        .receiveDate(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                        .minQty(r.getMinimumStockLevel())
                        .build();
                })
                .collect(Collectors.toList());

        double totalQty = batches.stream()
                .map(RawMaterial::getCurrentStock)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        return RawMaterialWithBatchesDto.builder()
                .id(base.getId())
                .code(materialCode)
                .materialName(base.getMaterialName())
                .name(resolveMaterialName(base))
                .brand(base.getBrandName())
                .genericMaterialName(base.getGenericMaterialName())
                .category(base.getCategory())
                .unit(base.getUnitOfMeasure())
                .batches(batchDtos)
                .totalQuantity(totalQty)
                .minQty(base.getMinimumStockLevel())
                .expireDate(null)
                .unitCost(base.getUnitCost())
                .build();
    }

    private String resolveMaterialName(RawMaterial base) {
        String genericName = base.getGenericMaterialName();
        String matName = base.getMaterialName();

        if (matName == null || matName.trim().isEmpty()) {
            return genericName != null ? genericName : "Raw Material";
        }

        boolean isPackOrUnitOnly = matName.trim().matches("(?i)^\\d+(\\.\\d+)?\\s*(kg|g|l|ml|unit|units|pcs|pack|packs)?$") ||
                                   matName.trim().matches("(?i)^\\d+\\s*(kg|g|l|ml|pack|packs)$") ||
                                   matName.trim().matches("(?i)^(1\\s*kg|1kg|100g\\s*pack|50kg\\s*pack|pack)$");

        if (isPackOrUnitOnly && genericName != null && !genericName.trim().isEmpty()) {
            return genericName;
        }

        if (genericName == null || genericName.trim().isEmpty()) {
            return matName;
        }

        if (matName.toLowerCase().contains(genericName.toLowerCase()) || 
            ("oil".equalsIgnoreCase(genericName.trim()) && matName.toLowerCase().contains("sunflower"))) {
            return matName;
        }

        return genericName;
    }

    @Override
    public PurchaseOrderRawMaterialsResponseDto getRawMaterialsByPurchaseOrderId(Long purchaseOrderId) {
        log.info("Retrieving raw materials for purchase order ID: {}", purchaseOrderId);

        // Validate purchase order exists
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(purchaseOrderId));

        // Get supplier information
        Optional<Supplier> supplierOpt = supplierRepository.findById(purchaseOrder.getSupplierId());
        String supplierName = supplierOpt.map(Supplier::getName).orElse("Unknown Supplier");

        // Lookup GRN for this purchase order (if any)
        Optional<Grn> grnOpt = grnRepository.findByPoId(purchaseOrderId);

        // Convert purchase order items to DTOs, enriching with grnItemId, batchNo, and grnExpireDate if present
        List<PurchaseOrderRawMaterialDto> rawMaterials = purchaseOrder.getPurchaseOrderItems().stream()
                .map(item -> {
                    PurchaseOrderRawMaterialDto dto = convertToPurchaseOrderRawMaterialDto(item);
                    Long grnItemId = null;
                    if (grnOpt.isPresent()) {
                        Long grnId = grnOpt.get().getGrnId();
                        List<GrnItem> items = grnItemRepository.findByGrnIdAndRawMaterialId(grnId, item.getRawMaterialId().longValue());
                        if (!items.isEmpty()) {
                            GrnItem grnItem = items.get(0);
                            grnItemId = grnItem.getGrnItemId();
                            dto.setBatchNo(grnItem.getBatchNo());
                            dto.setGrnExpireDate(grnItem.getExpireDate());
                            
                            // Fallback for older received GRNs that don't have batch details saved on GrnItem
                            if (dto.getBatchNo() == null || dto.getBatchNo().trim().isEmpty()) {
                                RawMaterial base = rawMaterialRepository.findById(grnItem.getRawMaterialId()).orElse(null);
                                if (base != null) {
                                    List<RawMaterial> batches = rawMaterialRepository.findAllByMaterialCode(base.getMaterialCode());
                                    Supplier supplier = grnOpt.get().getSupplier();
                                    String grnSupplierName = supplier != null ? supplier.getName() : supplierName;
                                    
                                    Optional<RawMaterial> matchedBatch = batches.stream()
                                            .filter(b -> b.getBatchNo() != null)
                                            .filter(b -> grnSupplierName != null && grnSupplierName.equalsIgnoreCase(b.getSupplierName()))
                                            .findFirst();
                                            
                                    if (matchedBatch.isPresent()) {
                                        dto.setBatchNo(matchedBatch.get().getBatchNo());
                                        dto.setGrnExpireDate(matchedBatch.get().getExpireDate());
                                    }
                                }
                            }

                            if (grnItem.getReceivedQuantity() != null) {
                                dto.setReceivedQty(grnItem.getReceivedQuantity().intValue());
                            }
                        }
                    }
                    dto.setGrnItemId(grnItemId);
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} raw materials for purchase order ID: {}", rawMaterials.size(), purchaseOrderId);

        return PurchaseOrderRawMaterialsResponseDto.builder()
                .purchaseOrderId(purchaseOrder.getPoId())
                .totalCost(purchaseOrder.getTotalCost())
                .numberOfItems(purchaseOrder.getNumberOfItems())
                .estimatedDeliveryDate(purchaseOrder.getEstimatedDeliveryDate())
                .supplierId(purchaseOrder.getSupplierId())
                .supplierName(supplierName)
                .rawMaterials(rawMaterials)
                .build();
    }

    @Override
    public AllRawMaterialsResponseDto getAllAvailableRawMaterials() {
        log.info("Retrieving all available raw materials with current stock > 0");

        List<RawMaterial> availableMaterials = rawMaterialRepository.findAll().stream()
                .filter(material -> material.getCurrentStock() != null && material.getCurrentStock() > 0)
                .collect(Collectors.toList());

        if (availableMaterials.isEmpty()) {
            log.warn("No raw materials found with current stock > 0");
            throw new RawMaterialsNotFoundException("No raw materials found with current stock greater than 0");
        }

        List<RawMaterialDto> rawMaterialDtos = availableMaterials.stream()
                .map(this::convertToRawMaterialDto)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} available raw materials", rawMaterialDtos.size());

        return AllRawMaterialsResponseDto.builder()
                .rawMaterials(rawMaterialDtos)
                .totalCount(rawMaterialDtos.size())
                .build();
    }

    /**
     * Converts a PurchaseOrderItem to PurchaseOrderRawMaterialDto.
     *
     * @param item the purchase order item to convert
     * @return the converted DTO
     */
    private PurchaseOrderRawMaterialDto convertToPurchaseOrderRawMaterialDto(PurchaseOrderItem item) {
        RawMaterial rawMaterial = item.getRawMaterial();

        return PurchaseOrderRawMaterialDto.builder()
                .rawMaterialId(rawMaterial.getId())
                .rawMaterialName(resolveMaterialName(rawMaterial))
                .currentStock(rawMaterial.getCurrentStock())
                .expireDate(rawMaterial.getExpireDate())
                .requiredQty(item.getRequiredQty())
                .receivedQty(item.getReceivedQty())
                .actualCost(item.getActualCost())
                .estimatedCost(item.getEstimatedCost())
                .unitOfMeasure(item.getUnitOfMeasure())
                .build();
    }

    /**
     * Converts a RawMaterial entity to RawMaterialDto.
     *
     * @param material the raw material entity to convert
     * @return the converted DTO
     */
    private RawMaterialDto convertToRawMaterialDto(RawMaterial rawMaterial) {
        return AllRawMaterialsResponseDto.RawMaterialDto.builder()
                .id(rawMaterial.getId())
                .materialName(resolveMaterialName(rawMaterial))
                .materialCode(rawMaterial.getMaterialCode())
                .description(rawMaterial.getDescription())
                .unitOfMeasure(rawMaterial.getUnitOfMeasure())
                .unitCost(rawMaterial.getUnitCost())
                .currentStock(rawMaterial.getCurrentStock())
                .minimumStockLevel(rawMaterial.getMinimumStockLevel())
                .category(rawMaterial.getCategory())
                .brand(rawMaterial.getBrandName())
                .genericMaterialName(rawMaterial.getBrand() != null && rawMaterial.getBrand().getGenericMaterial() != null 
                        ? rawMaterial.getBrand().getGenericMaterial().getName() : "Uncategorized")
                .brandName(rawMaterial.getBrandName())
                .isActive(rawMaterial.getIsActive())
                .expireDate(rawMaterial.getExpireDate())
                .build();
    }

    @Override
    public SupplierRawMaterialsResponseDto getRawMaterialsBySupplierId(Long supplierId) {
        log.info("Retrieving raw materials for supplier ID: {}", supplierId);

        // Validate supplier exists
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));

        // Get raw materials supplied by this supplier
        List<RawMaterialSupplier> rawMaterialSuppliers = rawMaterialSupplierRepository
                .findBySupplierId(supplierId);

        if (rawMaterialSuppliers.isEmpty()) {
            log.info("No raw materials found for supplier: {} (ID: {})", supplier.getName(), supplierId);
            return SupplierRawMaterialsResponseDto.builder()
                    .supplierId(supplierId)
                    .supplierName(supplier.getName())
                    .rawMaterials(Collections.emptyList())
                    .totalCount(0)
                    .build();
        }

        List<SupplierRawMaterialDto> rawMaterialDtos = new java.util.ArrayList<>();
        for (RawMaterialSupplier rms : rawMaterialSuppliers) {
            RawMaterial baseMaterial = rms.getRawMaterial();
            if (baseMaterial == null) {
                continue;
            }

            // Find all records (batches) in the database with the same material code
            List<RawMaterial> batches = rawMaterialRepository.findAllByMaterialCode(baseMaterial.getMaterialCode());

            // Filter batches that have positive stock and are active
            List<RawMaterial> activeBatches = batches.stream()
                    .filter(b -> b.getIsActive() != null && b.getIsActive() && b.getCurrentStock() != null && b.getCurrentStock() > 0)
                    .collect(Collectors.toList());

            // If no batches have positive stock, default to displaying the base material (or the newest batch) with 0 stock
            if (activeBatches.isEmpty()) {
                activeBatches = List.of(baseMaterial);
            }

            for (RawMaterial batch : activeBatches) {
                SupplierRawMaterialDto dto = SupplierRawMaterialDto.builder()
                        .rawMaterialId(batch.getId())
                        .rawMaterialName(resolveMaterialName(batch))
                        .expireDate(batch.getExpireDate())
                        .currentStock(batch.getCurrentStock())
                        .materialCode(batch.getMaterialCode())
                        .category(batch.getCategory())
                        .brand(batch.getBrandName())
                        .unitOfMeasure(batch.getUnitOfMeasure())
                        .unitCost(batch.getUnitCost())
                        .negotiatedUnitCost(rms.getNegotiatedUnitCost())
                        .leadTimeDays(rms.getLeadTimeDays())
                        .isPreferred(rms.getIsPreferred())
                        .batchNo(batch.getBatchNo())
                        .build();
                rawMaterialDtos.add(dto);
            }
        }

        log.info("Successfully retrieved {} raw material batches for supplier: {} (ID: {})",
                rawMaterialDtos.size(), supplier.getName(), supplierId);

        return SupplierRawMaterialsResponseDto.builder()
                .supplierId(supplierId)
                .supplierName(supplier.getName())
                .rawMaterials(rawMaterialDtos)
                .totalCount(rawMaterialDtos.size())
                .build();
    }
}
