package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.plover.backerymanagmentsystem.store_keeper.dto.MiniStoreDailyInventoryResponse;
import com.plover.backerymanagmentsystem.store_keeper.dto.MiniStoreDailyInventoryResponse.MiniStoreInventoryDetails;
import com.plover.backerymanagmentsystem.store_keeper.dto.MiniStoreDailyInventoryResponse.MiniStoreProductItem;
import com.plover.backerymanagmentsystem.store_keeper.dto.MiniStoreDailyInventoryResponse.MiniStoreRawMaterialItem;
import com.plover.backerymanagmentsystem.store_keeper.repository.MiniStoreInventoryRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.projection.MiniStoreInventoryProjection;
import com.plover.backerymanagmentsystem.store_keeper.service.MiniStoreInventoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MiniStoreInventoryServiceImpl implements MiniStoreInventoryService {

    private final MiniStoreInventoryRepository miniStoreInventoryRepository;

    @Override
    public MiniStoreDailyInventoryResponse getTodayInventoryByOutlet(Long outletId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<MiniStoreInventoryProjection> projections = miniStoreInventoryRepository.findDailyInventory(
                outletId, startOfDay, endOfDay);

        Map<Integer, MiniStoreInventoryDetails> groupedByMiniStore = new LinkedHashMap<>();

        for (MiniStoreInventoryProjection projection : projections) {
            Integer miniStoreId = projection.getMiniStoreId();
            MiniStoreInventoryDetails details = groupedByMiniStore.computeIfAbsent(miniStoreId, id -> MiniStoreInventoryDetails.builder()
                    .miniStoreId(id)
                    .miniStoreName(Objects.requireNonNullElse(projection.getMiniStoreName(), ""))
                    .rawMaterials(new ArrayList<>())
                    .products(new ArrayList<>())
                    .build());

            if (projection.getRawMaterialId() != null) {
                details.getRawMaterials().add(MiniStoreRawMaterialItem.builder()
                        .itemId(projection.getItemId())
                        .name(projection.getName())
                        .rawMaterialId(projection.getRawMaterialId())
                        .materialName(projection.getMaterialName())
                        .materialCode(projection.getMaterialCode())
                        .unitOfMeasure(projection.getUnitOfMeasure())
                        .systemQuantity(projection.getSystemQty())
                        .physicalQuantity(projection.getPhysicalQty())
                        .variance(projection.getVariance())
                        .updatedAt(projection.getUpdatedAt())
                        .build());
            }

            if (projection.getProductId() != null) {
                details.getProducts().add(MiniStoreProductItem.builder()
                        .itemId(projection.getItemId())
                        .name(projection.getName())
                        .productId(projection.getProductId())
                        .productName(projection.getProductName())
                        .productCode(projection.getProductCode())
                        .unitPrice(projection.getUnitPrice())
                        .systemQuantity(projection.getSystemQty())
                        .physicalQuantity(projection.getPhysicalQty())
                        .variance(projection.getVariance())
                        .updatedAt(projection.getUpdatedAt())
                        .build());
            }
        }

        return MiniStoreDailyInventoryResponse.builder()
                .outletId(outletId)
                .asOfDate(today)
                .miniStores(new ArrayList<>(groupedByMiniStore.values()))
                .build();
    }
}
