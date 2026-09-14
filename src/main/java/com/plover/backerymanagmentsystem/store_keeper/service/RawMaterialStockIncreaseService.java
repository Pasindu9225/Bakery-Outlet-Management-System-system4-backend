package com.plover.backerymanagmentsystem.store_keeper.service;

import java.math.BigDecimal;
import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveResponseDto.StockUpdateDto;

/**
 * Service interface for increasing raw material stock operations. This service
 * handles stock additions when goods are received.
 */
public interface RawMaterialStockIncreaseService {

    /**
     * Increases stock for a single raw material.
     *
     * @param rawMaterialId the raw material ID
     * @param quantity the quantity to add to current stock
     * @return the stock update information
     * @throws RawMaterialNotFoundException if material is not found or inactive
     */
    StockUpdateDto increaseStock(Long rawMaterialId, BigDecimal quantity);

    /**
     * Increases stock for multiple raw materials. Individual failures are
     * handled gracefully without affecting successful operations.
     *
     * @param stockIncreases list of material ID and quantity pairs
     * @return list of successful stock updates and any error messages
     */
    StockIncreaseResult bulkIncreaseStock(List<StockIncreaseRequest> stockIncreases);

    /**
     * Request object for stock increase operations.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class StockIncreaseRequest {

        private Long rawMaterialId;
        private BigDecimal quantity;
    }

    /**
     * Result object for bulk stock increase operations.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class StockIncreaseResult {

        private List<StockUpdateDto> successfulUpdates;
        private List<String> errors;
    }
}
