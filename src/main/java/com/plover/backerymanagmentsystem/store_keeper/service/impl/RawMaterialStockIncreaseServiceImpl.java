package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveResponseDto.StockUpdateDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialStockIncreaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of RawMaterialStockIncreaseService for handling stock
 * increases when goods are received through GRN operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RawMaterialStockIncreaseServiceImpl implements RawMaterialStockIncreaseService {

    private final RawMaterialRepository rawMaterialRepository;

    @Override
    @Transactional
    public StockUpdateDto increaseStock(Long rawMaterialId, BigDecimal quantity) {
        log.info("Increasing stock for material ID: {} by quantity: {}", rawMaterialId, quantity);

        // Find the raw material
        RawMaterial material = rawMaterialRepository.findById(rawMaterialId)
                .orElseThrow(() -> new RawMaterialNotFoundException(rawMaterialId));

        // Check if material is active
        if (!material.getIsActive()) {
            log.warn("Attempted to increase stock for inactive material ID: {}", rawMaterialId);
            throw new RawMaterialNotFoundException(rawMaterialId);
        }

        // Calculate new stock
        Double previousStock = material.getCurrentStock();
        Double addedQuantityDouble = quantity.doubleValue();
        Double newStock = previousStock + addedQuantityDouble;

        // Update stock
        material.setCurrentStock(newStock);
        rawMaterialRepository.save(material);

        // Check if still below minimum level
        boolean belowMinimumLevel = material.getMinimumStockLevel() != null
                && newStock < material.getMinimumStockLevel();

        log.info("Stock increased successfully for material {}: {} -> {}",
                material.getMaterialName(), previousStock, newStock);

        return StockUpdateDto.builder()
                .rawMaterialId(material.getId())
                .rawMaterialName(material.getMaterialName())
                .materialCode(material.getMaterialCode())
                .previousStock(previousStock)
                .addedQuantity(quantity)
                .newStock(newStock)
                .unitOfMeasure(material.getUnitOfMeasure())
                .belowMinimumLevel(belowMinimumLevel)
                .build();
    }

    @Override
    @Transactional
    public StockIncreaseResult bulkIncreaseStock(List<StockIncreaseRequest> stockIncreases) {
        log.info("Processing bulk stock increase for {} materials", stockIncreases.size());

        List<StockUpdateDto> successfulUpdates = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (StockIncreaseRequest request : stockIncreases) {
            try {
                StockUpdateDto result = increaseStock(request.getRawMaterialId(), request.getQuantity());
                successfulUpdates.add(result);
                log.debug("Successfully increased stock for material ID: {}", request.getRawMaterialId());
            } catch (RawMaterialNotFoundException e) {
                String errorMessage = String.format("Raw material with ID %d not found or inactive",
                        request.getRawMaterialId());
                errors.add(errorMessage);
                log.warn("Failed to increase stock for material ID: {} - {}",
                        request.getRawMaterialId(), errorMessage);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to increase stock for material ID %d: %s",
                        request.getRawMaterialId(), e.getMessage());
                errors.add(errorMessage);
                log.error("Unexpected error increasing stock for material ID: {}",
                        request.getRawMaterialId(), e);
            }
        }

        log.info("Bulk stock increase completed: {} successful, {} failed",
                successfulUpdates.size(), errors.size());

        return StockIncreaseResult.builder()
                .successfulUpdates(successfulUpdates)
                .errors(errors)
                .build();
    }
}
