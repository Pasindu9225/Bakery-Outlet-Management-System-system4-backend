package com.plover.backerymanagmentsystem.manager.service.impl;

import com.plover.backerymanagmentsystem.manager.dto.MiniStoreDto;
import com.plover.backerymanagmentsystem.manager.dto.MiniStoreItemDto;
import com.plover.backerymanagmentsystem.manager.dto.MiniStoreUpdateRequestDto;
import com.plover.backerymanagmentsystem.manager.model.MiniStore;
import com.plover.backerymanagmentsystem.manager.model.MiniStoreItem;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.service.MiniStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MiniStoreServiceImpl implements MiniStoreService {

    private final MiniStoreRepository miniStoreRepository;
    private final MiniStoreItemRepository miniStoreItemRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MiniStoreDto> getAllMiniStores() {
        log.info("Fetching all mini stores");
        List<MiniStore> miniStores = miniStoreRepository.findAllByOrderByStoreDateDesc();
        return miniStores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiniStoreItemDto> getMiniStoreItems(Integer miniStoreId) {
        log.info("Fetching mini store items for store ID: {}", miniStoreId);
        List<MiniStoreItem> items = miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStoreId);
        return items.stream()
                .filter(item -> item.getPhysicalQty() != null && item.getPhysicalQty().compareTo(java.math.BigDecimal.ZERO) > 0)
                .map(this::convertItemToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void updateMiniStoreItems(Integer miniStoreId, MiniStoreUpdateRequestDto updateRequest) {
        log.info("Updating mini store items for store ID: {}", miniStoreId);
        
        // Verify mini store exists
        MiniStore miniStore = miniStoreRepository.findById(miniStoreId)
                .orElseThrow(() -> new RuntimeException("Mini store not found with ID: " + miniStoreId));

        // Fetch existing items for this mini store to support incremental updates
        List<MiniStoreItem> existingItems = miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStoreId);

        // Process updates
        for (MiniStoreItemDto itemDto : updateRequest.getItems()) {
            MiniStoreItem itemToUpdate;

            // Try to find existing item
            if (itemDto.getItemId() != null) {
                itemToUpdate = existingItems.stream()
                        .filter(ei -> ei.getItemId().equals(itemDto.getItemId()))
                        .findFirst()
                        .orElse(null);
            } else {
                // Find by rawMaterialId or productId if itemId is not provided
                itemToUpdate = existingItems.stream()
                        .filter(ei -> (itemDto.getRawMaterialId() != null && itemDto.getRawMaterialId().equals(ei.getRawMaterialId())) ||
                                     (itemDto.getProductId() != null && itemDto.getProductId().equals(ei.getProductId())))
                        .findFirst()
                        .orElse(null);
            }

            if (itemToUpdate != null) {
                // Update existing item
                itemToUpdate.setPhysicalQty(itemDto.getPhysicalQty());
                itemToUpdate.setSystemQty(itemDto.getSystemQty());
                // Name might have changed? Optional but good to sync
                if (itemDto.getName() != null) {
                    itemToUpdate.setName(itemDto.getName());
                }
                miniStoreItemRepository.save(itemToUpdate);
            } else {
                // Create new item
                MiniStoreItem newItem = convertDtoToItem(itemDto);
                newItem.setMiniStore(miniStore);
                miniStoreItemRepository.save(newItem);
            }

            // Sync with Master Stock (RawMaterial)
            if (itemDto.getRawMaterialId() != null) {
                rawMaterialRepository.findById(itemDto.getRawMaterialId()).ifPresent(rm -> {
                    rm.setCurrentStock(itemDto.getPhysicalQty().doubleValue());
                    rawMaterialRepository.save(rm);
                    log.info("Synced master stock for raw material: {}", rm.getMaterialName());
                });
            }
        }
        
        log.info("Successfully updated items for mini store ID: {}", miniStoreId);
    }

    private MiniStoreDto convertToDto(MiniStore miniStore) {
        return MiniStoreDto.builder()
                .miniStoreId(miniStore.getMiniStoreId())
                .name(miniStore.getName())
                .storeDate(miniStore.getStoreDate())
                .createdAt(miniStore.getCreatedAt())
                .updatedAt(miniStore.getUpdatedAt())
                .build();
    }

    private MiniStoreItemDto convertItemToDto(MiniStoreItem item) {
        String brandName = "N/A";
        String genericName = "N/A";
        String unitOfMeasure = "pcs";

        if (item.getRawMaterialId() != null) {
            var rawMaterialOpt = rawMaterialRepository.findById(item.getRawMaterialId());
            if (rawMaterialOpt.isPresent()) {
                var rm = rawMaterialOpt.get();
                brandName = rm.getBrandName() != null ? rm.getBrandName() : "No Brand";
                genericName = rm.getGenericMaterialName() != null ? rm.getGenericMaterialName() : "Generic";
                if (rm.getUnitOfMeasure() != null && !rm.getUnitOfMeasure().isBlank()) {
                    unitOfMeasure = rm.getUnitOfMeasure();
                }
            }
        } else if (item.getProductId() != null) {
            genericName = "Product";
            var productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent()) {
                var p = productOpt.get();
                if (p.getBrand() != null && !p.getBrand().isBlank()) {
                    brandName = p.getBrand();
                }
                if (p.getUnitOfMeasure() != null && !p.getUnitOfMeasure().isBlank()) {
                    unitOfMeasure = p.getUnitOfMeasure();
                }
            }
        }

        return MiniStoreItemDto.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .rawMaterialId(item.getRawMaterialId())
                .systemQty(item.getSystemQty())
                .physicalQty(item.getPhysicalQty())
                .variance(item.getVariance())
                .miniStoreId(item.getMiniStore().getMiniStoreId())
                .outletId(item.getOutletId())
                .productId(item.getProductId())
                .brandName(brandName)
                .genericMaterialName(genericName)
                .unitOfMeasure(unitOfMeasure)
                .unit(unitOfMeasure)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private MiniStoreItem convertDtoToItem(MiniStoreItemDto dto) {
        return MiniStoreItem.builder()
                .itemId(dto.getItemId())
                .name(dto.getName())
                .rawMaterialId(dto.getRawMaterialId())
                .systemQty(dto.getSystemQty())
                .physicalQty(dto.getPhysicalQty())
                .variance(dto.getVariance())
                .outletId(dto.getOutletId())
                .productId(dto.getProductId())
                .build();
    }
}
