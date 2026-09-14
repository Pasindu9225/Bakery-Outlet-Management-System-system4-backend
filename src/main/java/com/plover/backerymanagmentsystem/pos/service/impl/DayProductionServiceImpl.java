package com.plover.backerymanagmentsystem.pos.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;
import com.plover.backerymanagmentsystem.pos.model.DayProduction;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionRepository;
import com.plover.backerymanagmentsystem.pos.repository.PosTableItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.PosWaiterItemRepository;
import com.plover.backerymanagmentsystem.pos.service.DayProductionService;

/**
 * Implementation of DayProductionService for managing day production operations
 */
@Service
@Transactional
public class DayProductionServiceImpl implements DayProductionService {

    private final DayProductionRepository dayProductionRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final PosTableItemRepository posTableItemRepository;
    private final PosWaiterItemRepository posWaiterItemRepository;

    public DayProductionServiceImpl(DayProductionRepository dayProductionRepository,
            DayProductionItemRepository dayProductionItemRepository,
            PosTableItemRepository posTableItemRepository,
            PosWaiterItemRepository posWaiterItemRepository) {
        this.dayProductionRepository = dayProductionRepository;
        this.dayProductionItemRepository = dayProductionItemRepository;
        this.posTableItemRepository = posTableItemRepository;
        this.posWaiterItemRepository = posWaiterItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DayProductionItemResponseDto> getTodayProductionItems(Long outletId) {
        LocalDate today = LocalDate.now();

        // Get today's active day production records for the specific outlet
        List<DayProduction> dayProductions = dayProductionRepository
                .findByOrderedDateAndOutletIdAndIsActiveTrue(today, outletId);

        if (dayProductions.isEmpty()) {
            return new ArrayList<>();
        }

        // Get production IDs
        List<Integer> productionIds = dayProductions.stream()
                .map(DayProduction::getProductionId)
                .collect(Collectors.toList());

        // Get production items for all today's batches with product details
        List<DayProductionItem> allDayProductionItems = dayProductionItemRepository
                .findByProductionIdInWithProduct(productionIds);

        // Filter to keep only the latest item for each product
        // Using LinkedHashMap to keep the order (highest dayProductionItemId first)
        Map<Long, DayProductionItem> latestItemsByProduct = new LinkedHashMap<>();

        // Sort items by ID descending so the first one we see for a product is the
        // latest
        allDayProductionItems.stream()
                .sorted((a, b) -> b.getDayProductionItemId().compareTo(a.getDayProductionItemId()))
                .forEachOrdered(item -> latestItemsByProduct.putIfAbsent(item.getProduct().getId(), item));

        // Convert to DTOs
        return latestItemsByProduct.values().stream()
                .map(item -> {
                    DayProductionItemResponseDto dto = DayProductionItemResponseDto.fromEntity(item);
                    Integer unpaidTableQty = posTableItemRepository.sumUnpaidQtyByDayProductionItemId(item.getDayProductionItemId());
                    Integer unpaidWaiterQty = posWaiterItemRepository.sumUnpaidQtyByDayProductionItemId(item.getDayProductionItemId());
                    int netAvailable = (item.getCurrentQty() != null ? item.getCurrentQty() : 0) - unpaidTableQty - unpaidWaiterQty;
                    dto.setCurrentQty(Math.max(0, netAvailable));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
