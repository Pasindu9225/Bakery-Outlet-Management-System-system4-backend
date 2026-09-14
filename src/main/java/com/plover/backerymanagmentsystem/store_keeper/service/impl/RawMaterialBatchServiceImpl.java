package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialBatchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of RawMaterialBatchService for creating new raw material batches.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RawMaterialBatchServiceImpl implements RawMaterialBatchService {

    private final RawMaterialRepository rawMaterialRepository;

    @Override
    public Long createNewBatch(Long existingMaterialId, String batchNo, BigDecimal receivedQuantity, String supplierName, BigDecimal unitCost, java.time.LocalDate expireDate) {
        log.info("Creating new batch for material ID: {} with batch number: {}, quantity: {}, unit cost: {}, expire date: {} from supplier: {}", 
                existingMaterialId, batchNo, receivedQuantity, unitCost, expireDate, supplierName);

        // Find the existing raw material to copy details from
        RawMaterial existingMaterial = rawMaterialRepository.findById(existingMaterialId)
                .orElseThrow(() -> new RuntimeException("Raw material not found with ID: " + existingMaterialId));

        // Update the base material's unit cost to reflect the latest price
        if (unitCost != null && unitCost.doubleValue() > 0) {
            existingMaterial.setUnitCost(unitCost.doubleValue());
            rawMaterialRepository.save(existingMaterial);
            log.info("Updated base material unit cost to {}", unitCost);
        }

        double finalUnitCost = (unitCost != null) ? unitCost.doubleValue() : (existingMaterial.getUnitCost() != null ? existingMaterial.getUnitCost() : 0.0);
        java.time.LocalDate finalExpireDate = (expireDate != null) ? expireDate : existingMaterial.getExpireDate();

        // If a batch with same material_code and batch number exists, update its current stock and initial quantity
        var existingBatchOpt = rawMaterialRepository.findByMaterialCodeAndBatchNo(
                existingMaterial.getMaterialCode(), batchNo);
        if (existingBatchOpt.isPresent()) {
            RawMaterial existingBatch = existingBatchOpt.get();
            double previous = existingBatch.getCurrentStock() != null ? existingBatch.getCurrentStock() : 0d;
            double previousInitial = existingBatch.getInitialQuantity() != null ? existingBatch.getInitialQuantity() : 0d;
            double added = receivedQuantity != null ? receivedQuantity.doubleValue() : 0d;
            
            existingBatch.setCurrentStock(previous + added);
            existingBatch.setInitialQuantity(previousInitial + added);
            existingBatch.setSupplierName(supplierName);
            existingBatch.setUnitCost(finalUnitCost);
            existingBatch.setExpireDate(finalExpireDate);
            existingBatch.setUpdatedAt(LocalDateTime.now());
            
            RawMaterial saved = rawMaterialRepository.save(existingBatch);
            log.info("Updated existing batch {} for material code {}. curr={}, init={}, unitCost={}, expireDate={}, supplier={}",
                    saved.getBatchNo(), saved.getMaterialCode(), saved.getCurrentStock(), saved.getInitialQuantity(), finalUnitCost, finalExpireDate, supplierName);
            return saved.getId();
        }

        // Create new batch with same details but different ID, batch number, and current stock
        RawMaterial newBatch = RawMaterial.builder()
                .brand(existingMaterial.getBrand()) // Link to same brand
                .materialName(existingMaterial.getMaterialName())
                .materialCode(existingMaterial.getMaterialCode())
                .unitOfMeasure(existingMaterial.getUnitOfMeasure())
                .unitCost(finalUnitCost)
                .currentStock(receivedQuantity.doubleValue())
                .initialQuantity(receivedQuantity.doubleValue())
                .supplierName(supplierName)
                .minimumStockLevel(existingMaterial.getMinimumStockLevel())
                .batchNo(batchNo)
                .isActive(true)
                .expireDate(finalExpireDate)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Save the new batch
        RawMaterial savedBatch = rawMaterialRepository.save(newBatch);
        
        log.info("Successfully created new batch with ID: {} for material: {} (material code: {}, batch: {}, unit cost: {})", 
                savedBatch.getId(), existingMaterial.getMaterialName(), existingMaterial.getMaterialCode(), batchNo, finalUnitCost);

        return savedBatch.getId();
    }
}
