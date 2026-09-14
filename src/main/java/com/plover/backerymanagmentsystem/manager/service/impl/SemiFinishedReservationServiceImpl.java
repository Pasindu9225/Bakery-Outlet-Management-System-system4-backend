package com.plover.backerymanagmentsystem.manager.service.impl;

import com.plover.backerymanagmentsystem.manager.dto.ReservationResult;
import com.plover.backerymanagmentsystem.manager.model.*;
import com.plover.backerymanagmentsystem.manager.repository.*;
import com.plover.backerymanagmentsystem.manager.service.SemiFinishedReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemiFinishedReservationServiceImpl implements SemiFinishedReservationService {

    private final SemiFinishedBatchInventoryRepository batchInventoryRepository;
    private final ProductionPlanBatchAllocationRepository allocationRepository;
    private final ProductRepository productRepository;
    private final MiniStoreItemRepository miniStoreItemRepository;
    private final MiniStoreRepository miniStoreRepository;

    @Override
    @Transactional
    public ReservationResult reserveSemiFinishedStock(Long productionPlanId, Long planItemId, Long productId, Long miniStoreId, Double requiredQty) {
        if (requiredQty == null || requiredQty <= 0) {
            return ReservationResult.builder()
                    .totalRequiredQty(0.0)
                    .reservedQty(0.0)
                    .netNeededQty(0.0)
                    .fullyFulfilled(true)
                    .allocations(new ArrayList<>())
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        List<SemiFinishedBatchInventory> fifoBatches;

        if (miniStoreId != null) {
            fifoBatches = batchInventoryRepository.findAvailableUnreservedBatchesFifo(productId, miniStoreId, now);
        } else {
            fifoBatches = batchInventoryRepository.findAvailableUnreservedBatchesFifoAnyStore(productId, now);
        }

        double remainingToReserve = requiredQty;
        double totalReserved = 0.0;
        List<ProductionPlanBatchAllocation> createdAllocations = new ArrayList<>();

        for (SemiFinishedBatchInventory batch : fifoBatches) {
            if (remainingToReserve <= 0.0001) {
                break;
            }

            double batchUnreserved = batch.getUnreservedQty();
            if (batchUnreserved <= 0) continue;

            double allocateQty = Math.min(remainingToReserve, batchUnreserved);
            batch.setReservedQty(batch.getReservedQty() + allocateQty);
            batchInventoryRepository.save(batch);

            ProductionPlanBatchAllocation allocation = ProductionPlanBatchAllocation.builder()
                    .productionPlanId(productionPlanId)
                    .planItemId(planItemId)
                    .batchInventory(batch)
                    .allocatedQty(allocateQty)
                    .status("RESERVED")
                    .build();
            
            createdAllocations.add(allocationRepository.save(allocation));

            totalReserved += allocateQty;
            remainingToReserve -= allocateQty;
        }

        double netNeeded = Math.max(0.0, requiredQty - totalReserved);
        boolean fullyFulfilled = (netNeeded <= 0.0001);

        log.info("Reserved stock for Plan #{}, Item #{}, Product #{}: required={}, reserved={}, netNeeded={}, fullyFulfilled={}",
                productionPlanId, planItemId, productId, requiredQty, totalReserved, netNeeded, fullyFulfilled);

        return ReservationResult.builder()
                .totalRequiredQty(requiredQty)
                .reservedQty(totalReserved)
                .netNeededQty(netNeeded)
                .fullyFulfilled(fullyFulfilled)
                .allocations(createdAllocations)
                .build();
    }

    @Override
    @Transactional
    public void releaseReservationsForPlan(Long productionPlanId) {
        List<ProductionPlanBatchAllocation> allocations = allocationRepository.findByProductionPlanIdAndStatus(productionPlanId, "RESERVED");
        for (ProductionPlanBatchAllocation allocation : allocations) {
            SemiFinishedBatchInventory batch = allocation.getBatchInventory();
            if (batch != null) {
                double newReserved = Math.max(0.0, batch.getReservedQty() - allocation.getAllocatedQty());
                batch.setReservedQty(newReserved);
                batchInventoryRepository.save(batch);
            }
            allocation.setStatus("RELEASED");
            allocationRepository.save(allocation);
        }
        log.info("Released {} reservations for ProductionPlan #{}", allocations.size(), productionPlanId);
    }

    @Override
    @Transactional
    public void consumeReservationsForPlanItem(Long planItemId) {
        List<ProductionPlanBatchAllocation> allocations = allocationRepository.findByPlanItemId(planItemId);
        for (ProductionPlanBatchAllocation allocation : allocations) {
            if ("RESERVED".equals(allocation.getStatus())) {
                SemiFinishedBatchInventory batch = allocation.getBatchInventory();
                if (batch != null) {
                    double newReserved = Math.max(0.0, batch.getReservedQty() - allocation.getAllocatedQty());
                    double newAvailable = Math.max(0.0, batch.getAvailableQty() - allocation.getAllocatedQty());
                    batch.setReservedQty(newReserved);
                    batch.setAvailableQty(newAvailable);
                    batchInventoryRepository.save(batch);

                    // Sync physicalQty in mini_store_items
                    syncMiniStoreItemQty(batch.getMiniStoreId(), batch.getProductId(), -allocation.getAllocatedQty());
                }
                allocation.setStatus("CONSUMED");
                allocationRepository.save(allocation);
            }
        }
    }

    @Override
    public Double getAvailableUnreservedStock(Long productId, Long miniStoreId) {
        LocalDateTime now = LocalDateTime.now();
        List<SemiFinishedBatchInventory> batches;
        if (miniStoreId != null) {
            batches = batchInventoryRepository.findAvailableUnreservedBatchesFifo(productId, miniStoreId, now);
        } else {
            batches = batchInventoryRepository.findAvailableUnreservedBatchesFifoAnyStore(productId, now);
        }

        return batches.stream()
                .mapToDouble(SemiFinishedBatchInventory::getUnreservedQty)
                .sum();
    }

    @Override
    public List<SemiFinishedBatchInventory> getBatchInventoryForMiniStore(Long miniStoreId) {
        if (miniStoreId == null) {
            return batchInventoryRepository.findAll();
        }
        return batchInventoryRepository.findByMiniStoreIdOrderByExpiryDateAsc(miniStoreId);
    }

    @Override
    @Transactional
    public SemiFinishedBatchInventory createBatchInventoryOnProduction(Long productId, String productName, Double initialQty, Long miniStoreId) {
        Product product = productRepository.findById(productId).orElse(null);
        int shelfLifeDays = (product != null && product.getShelfLifeDays() != null && product.getShelfLifeDays() > 0)
                ? product.getShelfLifeDays() : 3;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(shelfLifeDays);

        String batchNumber = "BATCH-" + productId + "-" + System.currentTimeMillis() % 100000;

        SemiFinishedBatchInventory batch = SemiFinishedBatchInventory.builder()
                .productId(productId)
                .productName(productName != null ? productName : (product != null ? product.getProductName() : "Product #" + productId))
                .batchNumber(batchNumber)
                .miniStoreId(miniStoreId != null ? miniStoreId : 1L)
                .initialQty(initialQty)
                .availableQty(initialQty)
                .reservedQty(0.0)
                .unitOfMeasure(product != null ? product.getUnitOfMeasure() : "kg")
                .manufacturedDate(now)
                .expiryDate(expiryDate)
                .build();

        batch = batchInventoryRepository.save(batch);

        // Sync MiniStoreItem physical stock
        syncMiniStoreItemQty(batch.getMiniStoreId(), productId, initialQty);

        log.info("Created semi-finished batch #{}: {} ({}) with initialQty={}, expiry={}",
                batch.getId(), batch.getProductName(), batch.getBatchNumber(), initialQty, expiryDate);

        return batch;
    }

    private void syncMiniStoreItemQty(Long miniStoreId, Long productId, Double deltaQty) {
        if (miniStoreId == null || productId == null || deltaQty == 0) return;
        
        List<MiniStoreItem> items = miniStoreItemRepository.findAll().stream()
                .filter(item -> item.getMiniStore() != null &&
                        miniStoreId.equals(item.getMiniStore().getMiniStoreId().longValue()) &&
                        productId.equals(item.getProductId()))
                .toList();

        if (!items.isEmpty()) {
            MiniStoreItem item = items.get(0);
            double currentQty = item.getPhysicalQty() != null ? item.getPhysicalQty().doubleValue() : 0.0;
            double newQty = Math.max(0.0, currentQty + deltaQty);
            item.setPhysicalQty(BigDecimal.valueOf(newQty));
            item.setSystemQty(BigDecimal.valueOf(newQty));
            miniStoreItemRepository.save(item);
        } else if (deltaQty > 0) {
            MiniStore miniStore = miniStoreRepository.findById(miniStoreId.intValue()).orElse(null);
            Product product = productRepository.findById(productId).orElse(null);
            if (miniStore != null) {
                MiniStoreItem newItem = MiniStoreItem.builder()
                        .name(product != null ? product.getProductName() : "Product #" + productId)
                        .productId(productId)
                        .miniStore(miniStore)
                        .physicalQty(BigDecimal.valueOf(deltaQty))
                        .systemQty(BigDecimal.valueOf(deltaQty))
                        .build();
                miniStoreItemRepository.save(newItem);
            }
        }
    }
}
