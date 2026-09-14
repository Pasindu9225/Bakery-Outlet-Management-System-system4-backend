package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.store_keeper.dto.LowStockMaterialsResponseDto.PurchaseOrderInfo;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.PurchaseOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of PurchaseOrderService. Handles purchase order operations
 * with proper business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public boolean isPurchaseOrderCreatedForMaterial(Integer rawMaterialId) {
        log.debug("Checking if purchase order exists for material ID: {}", rawMaterialId);

        try {
            boolean exists = purchaseOrderItemRepository.existsByRawMaterialId(rawMaterialId);
            log.debug("Purchase order exists for material ID {}: {}", rawMaterialId, exists);
            return exists;
        } catch (Exception e) {
            log.warn("Error checking purchase order for material ID {}: {}", rawMaterialId, e.getMessage());
            return false; // Default to false if there's an error (e.g., table doesn't exist)
        }
    }

    @Override
    public PurchaseOrderInfo getPurchaseOrderInfoForMaterial(Integer rawMaterialId) {
        log.debug("Retrieving purchase order info for material ID: {}", rawMaterialId);

        try {
            Optional<PurchaseOrderItem> purchaseOrderItemOpt
                    = purchaseOrderItemRepository.findMostRecentByRawMaterialId(rawMaterialId);

            if (purchaseOrderItemOpt.isEmpty()) {
                log.debug("No purchase order found for material ID: {}", rawMaterialId);
                return null;
            }

            PurchaseOrderItem purchaseOrderItem = purchaseOrderItemOpt.get();

            PurchaseOrderInfo info = PurchaseOrderInfo.builder()
                    .estimatedDeliveryDate(purchaseOrderItem.getPurchaseOrder().getEstimatedDeliveryDate()
                            .format(DATE_FORMATTER))
                    .totalCost(purchaseOrderItem.getEstimatedCost().doubleValue())
                    .requiredQty(purchaseOrderItem.getRequiredQty())
                    .receivedQty(purchaseOrderItem.getReceivedQty())
                    .build();

            log.debug("Found purchase order info for material ID {}: {}", rawMaterialId, info);
            return info;
        } catch (Exception e) {
            log.warn("Error retrieving purchase order info for material ID {}: {}", rawMaterialId, e.getMessage());
            return null; // Return null if there's an error (graceful degradation)
        }
    }
}
