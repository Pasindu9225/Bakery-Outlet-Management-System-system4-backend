package com.plover.backerymanagmentsystem.pos.service;

import java.util.List;

import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;

/**
 * Service interface for day production operations
 */
public interface DayProductionService {

    /**
     * Get today's production items with product details. This method retrieves
     * all production items for today's date, including product information
     * (name, code, unit price) and quantities (ordered and received).
     *
     * @return List of DayProductionItemResponseDto containing today's
     * production items
     */
    List<DayProductionItemResponseDto> getTodayProductionItems(Long outletId);
}
