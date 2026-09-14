package com.plover.backerymanagmentsystem.pos.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.pos.dto.DayEndClosingRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.DayEndClosingResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;
import com.plover.backerymanagmentsystem.pos.exception.DayEndException;
import com.plover.backerymanagmentsystem.pos.model.DayEndClosing;
import com.plover.backerymanagmentsystem.pos.model.DayEndClosingItem;
import com.plover.backerymanagmentsystem.pos.model.DayProduction;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.repository.DayEndClosingItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayEndClosingRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionRepository;
import com.plover.backerymanagmentsystem.pos.service.DayEndClosingService;
import com.plover.backerymanagmentsystem.pos.service.DayProductionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DayEndClosingServiceImpl implements DayEndClosingService {

    private final DayEndClosingRepository dayEndClosingRepository;
    private final DayEndClosingItemRepository dayEndClosingItemRepository;
    private final DayProductionRepository dayProductionRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final ProductRepository productRepository;
    private final DayProductionService dayProductionService;

    @Override
    public List<DayProductionItemResponseDto> getClosingInventory(Long outletId) {
        log.info("Fetching closing inventory for outlet: {}", outletId);
        return dayProductionService.getTodayProductionItems(outletId);
    }

    @Override
    @Transactional
    public DayEndClosingResponseDto submitClosing(DayEndClosingRequestDto request) {
        log.info("Submitting day-end stock closing for outlet ID: {}, cashier: {}", request.getOutletId(), request.getCashierId());

        LocalDate today = LocalDate.now();
        
        // 1. Validation: Handle duplicate submission gracefully (Idempotency)
        return dayEndClosingRepository.findByOutletIdAndClosingDate(request.getOutletId(), today)
                .map(existing -> {
                    log.info("Stock closing already exists for outlet {} on {}. Returning existing record ID: {}.", 
                        request.getOutletId(), today, existing.getId());
                    return DayEndClosingResponseDto.builder()
                            .success(true)
                            .message("Stock closing already submitted previously.")
                            .closingId(existing.getId())
                            .build();
                })
                .orElseGet(() -> performSubmitClosing(request, today));
    }

    private DayEndClosingResponseDto performSubmitClosing(DayEndClosingRequestDto request, LocalDate today) {
        log.info("Processing new stock closing for outlet {} on {}", request.getOutletId(), today);

        // 2. Save DayEndClosing header
        DayEndClosing closing = DayEndClosing.builder()
                .outletId(request.getOutletId())
                .cashierId(request.getCashierId())
                .closingDate(today)
                .build();
        
        closing = dayEndClosingRepository.save(closing);
        log.info("Saved DayEndClosing header with ID: {}", closing.getId());

        LocalDate tomorrow = today.plusDays(1);
        DayProduction tomorrowProduction = getOrCreateTomorrowProduction(tomorrow, request.getOutletId());

        List<DayEndClosingItem> closingItems = new ArrayList<>();

        if (request.getItems() != null) {
            for (DayEndClosingRequestDto.ClosingItemRequestDto itemReq : request.getItems()) {
                if (itemReq.getProductId() == null) continue;

                Integer system = itemReq.getSystemQty() != null ? itemReq.getSystemQty() : 0;
                Integer physical = itemReq.getPhysicalQty() != null ? itemReq.getPhysicalQty() : 0;
                Integer carryForward = itemReq.getCarryForwardQty() != null ? itemReq.getCarryForwardQty() : 0;
                Integer wastage = itemReq.getWastageQty() != null ? itemReq.getWastageQty() : 0;

                log.debug("Processing product {}: system={}, physical={}, carryForward={}, wastage={}", 
                    itemReq.getProductId(), system, physical, carryForward, wastage);

                // Validation: wastage + carry forward = physical
                if (!physical.equals(carryForward + wastage)) {
                    log.warn("Quantity mismatch for product {}: physical ({}) != cf ({}) + wastage ({})", 
                        itemReq.getProductId(), physical, carryForward, wastage);
                    throw new DayEndException("Validation failed for product ID " + itemReq.getProductId() + 
                        ": Physical quantity (" + physical + ") must equal Carry Forward (" + 
                        carryForward + ") + Wastage (" + wastage + ")");
                }

                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new DayEndException("Product not found: " + itemReq.getProductId()));

                // Update today's wastage in DayProductionItem
                updateTodayWastage(itemReq.getProductId(), request.getOutletId(), wastage);

                // Log details in DayEndClosingItem
                DayEndClosingItem closingItem = DayEndClosingItem.builder()
                        .dayEndClosing(closing)
                        .product(product)
                        .systemQty(system)
                        .physicalQty(physical)
                        .carryForwardQty(carryForward)
                        .wastageQty(wastage)
                        .build();

                closingItems.add(closingItem);

                // Carry Forward logic: seed tomorrow's inventory
                if (carryForward > 0) {
                    seedTomorrowInventory(tomorrowProduction, product, carryForward);
                }
            }
        }

        if (!closingItems.isEmpty()) {
            dayEndClosingItemRepository.saveAll(closingItems);
            log.info("Saved {} closing items", closingItems.size());
        }

        log.info("Stock closing completed successfully with ID: {}", closing.getId());

        return DayEndClosingResponseDto.builder()
                .success(true)
                .message("End-of-day stock closing processed successfully")
                .closingId(closing.getId())
                .build();
    }

    private void updateTodayWastage(Long productId, Long outletId, Integer wastageQty) {
        log.debug("Updating today's wastage for product {} in outlet {}: {}", productId, outletId, wastageQty);
        // Find latest today's record for this product and outlet
        dayProductionItemRepository.findFirstByProduct_IdAndOutletIdOrderByDayProductionItemIdDesc(productId, outletId)
                .stream().findFirst()
                .ifPresent(dpi -> {
                    if (dpi.getDayProduction() != null && 
                        dpi.getDayProduction().getOrderedDate() != null && 
                        dpi.getDayProduction().getOrderedDate().equals(LocalDate.now())) {
                        dpi.setWastageQty(wastageQty);
                        dayProductionItemRepository.save(dpi);
                        log.debug("Updated wastage for product {} in today's production plan for outlet {}", productId, outletId);
                    } else {
                        log.debug("No active production record found for product {} on today's date in outlet {}. Skipping wastage update.", productId, outletId);
                    }
                });
    }

    private DayProduction getOrCreateTomorrowProduction(LocalDate tomorrow, Long outletId) {
        List<DayProduction> productions = dayProductionRepository.findByOrderedDateAndOutletIdAndIsActiveTrue(tomorrow, outletId);
        if (!productions.isEmpty()) {
            log.debug("Found existing production plan for {} in outlet {}", tomorrow, outletId);
            return productions.get(0);
        }

        log.info("Creating new production plan for {} in outlet {}", tomorrow, outletId);
        DayProduction newProduction = DayProduction.builder()
                .orderedDate(tomorrow)
                .outletId(outletId)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        return dayProductionRepository.save(newProduction);
    }

    private void seedTomorrowInventory(DayProduction tomorrowProduction, Product product, Integer carryForwardQty) {
        // Check if item already exists in tomorrow's production to avoid duplicates/constraints
        boolean exists = false;
        if (tomorrowProduction.getDayProductionItems() != null) {
            exists = tomorrowProduction.getDayProductionItems().stream()
                    .anyMatch(item -> item.getProduct() != null && item.getProduct().getId().equals(product.getId()));
        }

        if (exists) {
            log.info("Product {} already exists in tomorrow's production plan. Skipping seeding.", product.getProductName());
            return;
        }

        DayProductionItem tomorrowItem = DayProductionItem.builder()
                .product(product)
                .dayProduction(tomorrowProduction)
                .orderedQty(0)
                .receivedQty(carryForwardQty)
                .currentQty(carryForwardQty) // This becomes the opening stock for tomorrow
                .createdAt(LocalDateTime.now())
                .build();
        
        dayProductionItemRepository.save(tomorrowItem);
        log.info("Carried forward {} units of product {} to tomorrow ({})", carryForwardQty, product.getProductName(), tomorrowProduction.getOrderedDate());
    }
}
