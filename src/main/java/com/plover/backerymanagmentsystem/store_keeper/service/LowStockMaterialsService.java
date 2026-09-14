package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.LowStockMaterialsResponseDto;

/**
 * Service interface for managing low stock materials operations. Follows Single
 * Responsibility Principle by handling only low stock material queries.
 */
public interface LowStockMaterialsService {

    /**
     * Retrieves all materials that are low on stock or out of stock.
     *
     * @return LowStockMaterialsResponseDto containing all low stock materials
     * with detailed information
     */
    LowStockMaterialsResponseDto getLowStockMaterials();
}
