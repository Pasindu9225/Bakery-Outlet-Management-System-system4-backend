package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.LowStockMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.LowStockMaterialsResponseDto.PurchaseOrderInfo;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialSupplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialSupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.LowStockMaterialsService;
import com.plover.backerymanagmentsystem.store_keeper.service.PurchaseOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of LowStockMaterialsService. Handles comprehensive low stock
 * material identification with proper business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LowStockMaterialsServiceImpl implements LowStockMaterialsService {

    private final RawMaterialRepository rawMaterialRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final RawMaterialSupplierRepository rawMaterialSupplierRepository;

    @Override
    public LowStockMaterialsResponseDto getLowStockMaterials() {
        log.info("Retrieving low stock materials");

        // Get all raw materials
        List<RawMaterial> allMaterials = rawMaterialRepository.findAll();

        // Group by material code to aggregate batches
        java.util.Map<String, List<RawMaterial>> byCode = allMaterials.stream()
                .collect(Collectors.groupingBy(RawMaterial::getMaterialCode));

        List<LowStockMaterialsResponseDto.LowStockMaterialDto> lowStockMaterials = byCode.entrySet().stream()
                .map(entry -> {
                    List<RawMaterial> batches = entry.getValue();
                    // Find the base material (e.g. batchNo is null or lowest ID)
                    RawMaterial baseMaterial = batches.stream()
                            .filter(m -> m.getBatchNo() == null)
                            .findFirst()
                            .orElse(batches.get(0));

                    // Aggregate stock
                    double totalStock = batches.stream()
                            .map(RawMaterial::getCurrentStock)
                            .filter(java.util.Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .sum();

                    return mapToLowStockMaterialDto(baseMaterial, totalStock);
                })
                .sorted(java.util.Comparator.comparing(LowStockMaterialsResponseDto.LowStockMaterialDto::getMaterialName, java.util.Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        log.info("Found {} aggregated materials with low or zero stock", lowStockMaterials.size());

        return LowStockMaterialsResponseDto.builder()
                .lowStockMaterials(lowStockMaterials)
                .totalCount(lowStockMaterials.size())
                .build();
    }

    /**
     * Determines if a material qualifies as low stock based on business rules:
     * 1. Current stock <= minimum stock level (when minimum is set) 2. Current
     * stock = 0 (out of stock, regardless of minimum stock setting)
     */
    private boolean isLowStockMaterial(RawMaterial material) {
        Double currentStock = material.getCurrentStock();
        Double minimumStockLevel = material.getMinimumStockLevel();

        // Rule 1: Out of stock (current stock = 0)
        if (currentStock == null || currentStock <= 0) {
            return true;
        }

        // Rule 2: Below minimum stock level (when minimum is set)
        if (minimumStockLevel != null && minimumStockLevel > 0) {
            return currentStock <= minimumStockLevel;
        }

        return false;
    }

    /**
     * Maps RawMaterial entity to LowStockMaterialDto with comprehensive
     * information
     */
    private LowStockMaterialsResponseDto.LowStockMaterialDto mapToLowStockMaterialDto(RawMaterial material, double aggregatedStock) {
        Double currentStock = aggregatedStock;
        Double minimumStockLevel = material.getMinimumStockLevel();

        // Calculate stock status and deficit
        StockAnalysis analysis = analyzeStockStatus(currentStock, minimumStockLevel, material.getIsActive());

        // Get purchase order information
        Integer rawMaterialId = material.getId().intValue();
        boolean isPoCreated = purchaseOrderService.isPurchaseOrderCreatedForMaterial(rawMaterialId);
        PurchaseOrderInfo purchaseOrderInfo = null;

        if (isPoCreated) {
            purchaseOrderInfo = purchaseOrderService.getPurchaseOrderInfoForMaterial(rawMaterialId);
        }

        // Get suppliers
        List<RawMaterialSupplier> materialSuppliers = rawMaterialSupplierRepository.findByRawMaterialIdWithDetails(material.getId());
        List<LowStockMaterialsResponseDto.SupplierDetail> supplierDetails = materialSuppliers.stream()
                .map(rms -> LowStockMaterialsResponseDto.SupplierDetail.builder()
                        .supplierId(rms.getSupplier().getSupplierId())
                        .name(rms.getSupplier().getName())
                        .brand(material.getBrandName())
                        .build())
                .collect(Collectors.toList());

        return LowStockMaterialsResponseDto.LowStockMaterialDto.builder()
                .materialId(material.getId())
                .materialName(material.getMaterialName())
                .materialCode(material.getMaterialCode())
                .category(material.getCategory())
                .currentStock(currentStock)
                .minimumStockLevel(minimumStockLevel)
                .maxStockLevel(material.getMaxStockLevel())
                .stockDeficit(analysis.stockDeficit)
                .unitCost(material.getUnitCost())
                .unitOfMeasure(material.getUnitOfMeasure())
                .isActive(material.getIsActive())
                .stockStatus(analysis.status)
                .notes(analysis.notes)
                .brand(material.getBrandName())
                .genericMaterialName(material.getGenericMaterialName())
                .suppliers(supplierDetails)
                .isPoCreated(isPoCreated)
                .purchaseOrderInfo(purchaseOrderInfo)
                .build();
    }

    /**
     * Analyzes stock status and provides detailed information
     */
    private StockAnalysis analyzeStockStatus(Double currentStock, Double minimumStockLevel, Boolean isActive) {
        String status;
        String notes;
        Double stockDeficit = 0.0;

        // Handle inactive materials
        String activeStatus = (isActive != null && isActive) ? "" : " (Material is inactive)";

        if (currentStock <= 0) {
            status = "OUT_OF_STOCK";
            notes = "Material is completely out of stock" + activeStatus;

            if (minimumStockLevel != null && minimumStockLevel > 0) {
                stockDeficit = minimumStockLevel - currentStock;
                notes += String.format(". Minimum stock level is %.2f %s", minimumStockLevel,
                        minimumStockLevel == 1 ? "unit" : "units");
            } else {
                notes += ". No minimum stock level set up";
            }
        } else if (minimumStockLevel == null || minimumStockLevel <= 0) {
            status = "NO_MINIMUM_SET";
            notes = "Material has stock but no minimum stock level set up" + activeStatus;
            stockDeficit = 0.0;
        } else if (currentStock <= minimumStockLevel) {
            status = "LOW_STOCK";
            stockDeficit = minimumStockLevel - currentStock;
            notes = String.format("Current stock (%.2f) is below minimum level (%.2f). Deficit: %.2f %s%s",
                    currentStock, minimumStockLevel, stockDeficit,
                    stockDeficit == 1 ? "unit" : "units", activeStatus);
        } else {
            // This shouldn't happen due to filtering, but added for completeness
            status = "ADEQUATE_STOCK";
            notes = "Stock level is adequate" + activeStatus;
            stockDeficit = 0.0;
        }

        return new StockAnalysis(status, notes, stockDeficit);
    }

    /**
     * Internal class to hold stock analysis results
     */
    private static class StockAnalysis {

        final String status;
        final String notes;
        final Double stockDeficit;

        StockAnalysis(String status, String notes, Double stockDeficit) {
            this.status = status;
            this.notes = notes;
            this.stockDeficit = stockDeficit;
        }
    }
}
