package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.BulkReduceRawMaterialStockRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.BulkReduceRawMaterialStockResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ReduceRawMaterialStockRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ReduceRawMaterialStockResponseDto;

/**
 * Service interface for managing raw material stock operations. Handles stock
 * reductions with proper validation and error handling.
 */
public interface RawMaterialStockService {

    /**
     * Reduces stock for a single raw material.
     *
     * @param request the stock reduction request
     * @return the stock reduction response with updated stock information
     * @throws RawMaterialNotFoundException if material is not found
     * @throws InsufficientStockException if requested quantity exceeds
     * available stock
     */
    ReduceRawMaterialStockResponseDto reduceStock(ReduceRawMaterialStockRequestDto request);

    /**
     * Reduces stock for multiple raw materials in a single transaction. Failed
     * reductions are recovered individually while successful ones are kept.
     *
     * @param request the bulk stock reduction request
     * @return the bulk stock reduction response with successful and failed
     * operations
     */
    BulkReduceRawMaterialStockResponseDto bulkReduceStock(BulkReduceRawMaterialStockRequestDto request);
}
