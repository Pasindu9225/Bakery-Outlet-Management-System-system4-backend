package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkReduceRawMaterialStockRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkReduceRawMaterialStockResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ReduceRawMaterialStockRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ReduceRawMaterialStockResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.InsufficientStockException;
import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialStockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RawMaterialStockServiceImpl implements RawMaterialStockService {

    private final RawMaterialRepository rawMaterialRepository;

    @Override
    @Transactional
    public ReduceRawMaterialStockResponseDto reduceStock(ReduceRawMaterialStockRequestDto request) {
        log.info("Reducing stock for material ID: {} by quantity: {}", request.getMaterialId(), request.getQuantity());

        // Find the raw material
        RawMaterial material = rawMaterialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new RawMaterialNotFoundException(request.getMaterialId()));

        // Check if material is active
        if (!material.getIsActive()) {
            throw new RawMaterialNotFoundException(request.getMaterialId());
        }

        // Validate sufficient stock
        if (material.getCurrentStock() < request.getQuantity()) {
            log.warn("Insufficient stock for material {}: requested {}, available {}",
                    material.getMaterialName(), request.getQuantity(), material.getCurrentStock());
            throw new InsufficientStockException(List.of(
                    String.format("%s (ID: %d) - Requested: %.2f, Available: %.2f",
                            material.getMaterialName(), material.getId(),
                            request.getQuantity(), material.getCurrentStock())
            ));
        }

        // Calculate new stock
        Double previousStock = material.getCurrentStock();
        Double newStock = previousStock - request.getQuantity();

        // Update stock
        material.setCurrentStock(newStock);
        rawMaterialRepository.save(material);

        // Check if below minimum level
        boolean belowMinimumLevel = material.getMinimumStockLevel() != null
                && newStock < material.getMinimumStockLevel();

        String message = belowMinimumLevel
                ? "Warning: Stock is now below minimum level"
                : "Stock reduced successfully";

        log.info("Stock reduced successfully for material {}: {} -> {}",
                material.getMaterialName(), previousStock, newStock);

        return ReduceRawMaterialStockResponseDto.builder()
                .materialId(material.getId())
                .materialName(material.getMaterialName())
                .materialCode(material.getMaterialCode())
                .previousStock(previousStock)
                .reducedQuantity(request.getQuantity())
                .newStock(newStock)
                .unitOfMeasure(material.getUnitOfMeasure())
                .minimumStockLevel(material.getMinimumStockLevel())
                .belowMinimumLevel(belowMinimumLevel)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public BulkReduceRawMaterialStockResponseDto bulkReduceStock(BulkReduceRawMaterialStockRequestDto request) {
        log.info("Processing bulk stock reduction for {} materials", request.getMaterialReductions().size());

        List<ReduceRawMaterialStockResponseDto> successfulReductions = new ArrayList<>();
        List<BulkReduceRawMaterialStockResponseDto.StockReductionError> failedReductions = new ArrayList<>();

        for (ReduceRawMaterialStockRequestDto reduction : request.getMaterialReductions()) {
            try {
                ReduceRawMaterialStockResponseDto response = reduceStock(reduction);
                successfulReductions.add(response);
                log.debug("Successfully reduced stock for material ID: {}", reduction.getMaterialId());
            } catch (RawMaterialNotFoundException e) {
                log.warn("Material not found during bulk reduction: {}", reduction.getMaterialId());
                failedReductions.add(BulkReduceRawMaterialStockResponseDto.StockReductionError.builder()
                        .materialId(reduction.getMaterialId())
                        .materialName("Unknown")
                        .requestedQuantity(reduction.getQuantity())
                        .errorMessage(e.getMessage())
                        .errorType("MATERIAL_NOT_FOUND")
                        .build());
            } catch (InsufficientStockException e) {
                log.warn("Insufficient stock during bulk reduction for material ID: {}", reduction.getMaterialId());
                failedReductions.add(BulkReduceRawMaterialStockResponseDto.StockReductionError.builder()
                        .materialId(reduction.getMaterialId())
                        .materialName("Unknown") // We could fetch this but keeping it simple for now
                        .requestedQuantity(reduction.getQuantity())
                        .errorMessage(e.getMessage())
                        .errorType("INSUFFICIENT_STOCK")
                        .build());
            } catch (Exception e) {
                log.error("Unexpected error during bulk stock reduction for material ID: {}", reduction.getMaterialId(), e);
                failedReductions.add(BulkReduceRawMaterialStockResponseDto.StockReductionError.builder()
                        .materialId(reduction.getMaterialId())
                        .materialName("Unknown")
                        .requestedQuantity(reduction.getQuantity())
                        .errorMessage("Unexpected error: " + e.getMessage())
                        .errorType("SYSTEM_ERROR")
                        .build());
            }
        }

        String summary = String.format("Processed %d materials: %d successful, %d failed",
                request.getMaterialReductions().size(), successfulReductions.size(), failedReductions.size());

        log.info("Bulk stock reduction completed: {}", summary);

        return BulkReduceRawMaterialStockResponseDto.builder()
                .successfulReductions(successfulReductions)
                .failedReductions(failedReductions)
                .summary(summary)
                .build();
    }
}
