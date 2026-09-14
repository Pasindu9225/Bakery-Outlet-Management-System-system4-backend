package com.plover.backerymanagmentsystem.worker.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.manager.model.ProductionBatch;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.repository.ProductionBatchRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository;
import com.plover.backerymanagmentsystem.worker.dto.ProductionBatchDto;
import com.plover.backerymanagmentsystem.worker.dto.ProductionItemProgressDto;
import com.plover.backerymanagmentsystem.worker.dto.RecordBatchDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerProductionTrackingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import com.plover.backerymanagmentsystem.manager.model.MiniStore;
import com.plover.backerymanagmentsystem.manager.model.MiniStoreItem;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkerProductionTrackingServiceImpl implements WorkerProductionTrackingService {

    private final ProductionBatchRepository productionBatchRepository;
    private final ProductionPlanItemRepository productionPlanItemRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final MiniStoreItemRepository miniStoreItemRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository billOfMaterialRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository rawMaterialRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.RecipeRepository recipeRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.RecipeIngredientRepository recipeIngredientRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.ActualProductionHistoryRepository actualProductionHistoryRepository;
    private final com.plover.backerymanagmentsystem.manager.service.SemiFinishedReservationService semiFinishedReservationService;

    @Override
    public ProductionBatchDto recordBatch(RecordBatchDto dto) {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);

        ProductionPlanItem item = productionPlanItemRepository.findById(dto.getProductionPlanItemId())
                .orElseThrow(() -> new RuntimeException("Production plan item not found: " + dto.getProductionPlanItemId()));

        if (pcId != null && item.getProductionCenterId() != null && !pcId.equals(item.getProductionCenterId())) {
            log.warn("Worker {} (pcId={}) recording batch for item {} assigned to center {}", user.getUsername(), pcId, item.getId(), item.getProductionCenterId());
        }

        // Dependency Gating: Verify all required child sub-assemblies are completed before producing this item
        List<ProductionPlanItem> childItems = productionPlanItemRepository.findByParentPlanItemId(item.getId());
        if (!childItems.isEmpty()) {
            for (ProductionPlanItem child : childItems) {
                int childProduced = productionBatchRepository.findByProductionPlanItemIdOrderByCreatedAtDesc(child.getId())
                        .stream().mapToInt(ProductionBatch::getProducedQty).sum();
                if (child.getQuantity() != null && childProduced < child.getQuantity()) {
                    String centerName = "Unknown Center";
                    if (child.getProductionCenterId() != null) {
                        ProductionCenter center = productionCenterRepository.findById(child.getProductionCenterId()).orElse(null);
                        if (center != null) centerName = center.getCenterName();
                    }
                    throw new RuntimeException("Cannot produce '" + item.getProductName() + "': Required child item '" 
                            + child.getProductName() + "' in " + centerName + " is not completed yet (Produced: " 
                            + childProduced + "/" + child.getQuantity() + ").");
                }
            }
        }

        // Sum existing produced batches
        List<ProductionBatch> existing = productionBatchRepository
                .findByProductionPlanItemIdOrderByCreatedAtDesc(item.getId());
        int alreadyProduced = existing.stream().mapToInt(ProductionBatch::getProducedQty).sum();

        int wastageQty = dto.getWastageQty() != null ? dto.getWastageQty() : 0;
        if (wastageQty > dto.getProducedQty()) {
            throw new RuntimeException("wastage cannot exceed produced");
        }
        if (wastageQty > 0 && (dto.getWastageReason() == null || dto.getWastageReason().isBlank())) {
            throw new RuntimeException("wastage reason is required when wastage > 0");
        }

        byte[] producedByBytes = user.getId();

        ProductionBatch batch = ProductionBatch.builder()
                .productionPlanItemId(item.getId())
                .producedQty(dto.getProducedQty())
                .wastageQty(wastageQty)
                .wastageReason(dto.getWastageReason())
                .producedBy(producedByBytes)
                .productionCenterId(pcId)
                .notes(dto.getNotes())
                .build();

        ProductionBatch saved = productionBatchRepository.save(batch);
        log.info("Worker {} recorded batch for item {} in PC {}: produced={}, wastage={}",
                user.getUsername(), item.getId(), pcId, dto.getProducedQty(), wastageQty);

        // Upsert produced item (product / semi-finished product) into MiniStoreItem for this Production Center
        if (dto.getProducedQty() > 0) {
            ProductionCenter center = productionCenterRepository.findById(pcId).orElse(null);
            MiniStore miniStore = (center != null) ? center.getMiniStore() : null;
            if (miniStore != null) {
                if (item.getProductId() != null) {
                    // Create batch inventory with shelf-life expiry tracking and consume reservations.
                    // semiFinishedReservationService.createBatchInventoryOnProduction syncs MiniStoreItem stock.
                    Long storeId = miniStore.getMiniStoreId() != null ? miniStore.getMiniStoreId().longValue() : 1L;
                    semiFinishedReservationService.createBatchInventoryOnProduction(item.getProductId(), item.getProductName(), (double) dto.getProducedQty(), storeId);
                    semiFinishedReservationService.consumeReservationsForPlanItem(item.getId());
                } else {
                    List<MiniStoreItem> items = miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStore.getMiniStoreId());
                    MiniStoreItem existingStoreItem = items.stream()
                            .filter(i -> item.getProductName() != null && item.getProductName().equalsIgnoreCase(i.getName()))
                            .findFirst().orElse(null);

                    BigDecimal qtyBD = BigDecimal.valueOf(dto.getProducedQty());
                    if (existingStoreItem != null) {
                        existingStoreItem.setSystemQty(existingStoreItem.getSystemQty() != null ? existingStoreItem.getSystemQty().add(qtyBD) : qtyBD);
                        existingStoreItem.setPhysicalQty(existingStoreItem.getPhysicalQty() != null ? existingStoreItem.getPhysicalQty().add(qtyBD) : qtyBD);
                        existingStoreItem.setName(item.getProductName());
                        miniStoreItemRepository.save(existingStoreItem);
                    } else {
                        MiniStoreItem newItem = MiniStoreItem.builder()
                                .name(item.getProductName())
                                .productId(item.getProductId())
                                .systemQty(qtyBD)
                                .physicalQty(qtyBD)
                                .miniStore(miniStore)
                                .rawMaterialId(null)
                                .outletId(null)
                                .build();
                        miniStoreItemRepository.save(newItem);
                    }
                }

                // Deduct child plan items (sub-assemblies), BOM components, and Recipe ingredients from MiniStore
                List<MiniStoreItem> items = miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStore.getMiniStoreId());
                deductIngredientsFromMiniStore(item, BigDecimal.valueOf(dto.getProducedQty()), miniStore, items);
            }

            // Save production history record
            if (item.getProductId() != null) {
                com.plover.backerymanagmentsystem.manager.model.ActualProductionHistory history = 
                        com.plover.backerymanagmentsystem.manager.model.ActualProductionHistory.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(dto.getProducedQty())
                        .actionType(com.plover.backerymanagmentsystem.manager.model.ActualProductionHistoryAction.PRODUCTION_IN)
                        .referenceName(item.getProductionPlan() != null ? item.getProductionPlan().getPlanName() : "Batch Production")
                        .build();
                actualProductionHistoryRepository.save(history);
            }
        }

        int newTotalProduced = alreadyProduced + dto.getProducedQty();
        int remaining = Math.max(0, item.getQuantity() - newTotalProduced);

        return ProductionBatchDto.builder()
                .id(saved.getId())
                .productionPlanItemId(item.getId())
                .productName(item.getProductName())
                .producedQty(saved.getProducedQty())
                .wastageQty(saved.getWastageQty())
                .wastageReason(saved.getWastageReason())
                .plannedQty(item.getQuantity())
                .remainingQty(remaining)
                .notes(saved.getNotes())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionItemProgressDto getItemProgress(Long productionPlanItemId) {
        ProductionPlanItem item = productionPlanItemRepository.findById(productionPlanItemId)
                .orElseThrow(() -> new RuntimeException("Production plan item not found: " + productionPlanItemId));

        List<ProductionBatch> batches = productionBatchRepository
                .findByProductionPlanItemIdOrderByCreatedAtDesc(item.getId());

        int totalProduced = batches.stream().mapToInt(ProductionBatch::getProducedQty).sum();
        int totalWastage = batches.stream().mapToInt(ProductionBatch::getWastageQty).sum();
        int remaining = Math.max(0, item.getQuantity() - totalProduced);

        List<ProductionBatchDto> batchDtos = batches.stream().map(b -> ProductionBatchDto.builder()
                .id(b.getId())
                .productionPlanItemId(b.getProductionPlanItemId())
                .productName(item.getProductName())
                .producedQty(b.getProducedQty())
                .wastageQty(b.getWastageQty())
                .wastageReason(b.getWastageReason())
                .plannedQty(item.getQuantity())
                .remainingQty(remaining)
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build()).collect(Collectors.toList());

        return ProductionItemProgressDto.builder()
                .productionPlanItemId(item.getId())
                .productionPlanId(item.getProductionPlan() != null ? item.getProductionPlan().getId() : null)
                .productName(item.getProductName())
                .plannedQty(item.getQuantity())
                .producedQty(totalProduced)
                .wastageQty(totalWastage)
                .remainingQty(remaining)
                .batches(batchDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductionBatchDto> listMyBatches() {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);

        return productionBatchRepository.findByProductionCenterIdOrderByCreatedAtDesc(pcId)
                .stream()
                .map(b -> ProductionBatchDto.builder()
                        .id(b.getId())
                        .productionPlanItemId(b.getProductionPlanItemId())
                        .producedQty(b.getProducedQty())
                        .wastageQty(b.getWastageQty())
                        .wastageReason(b.getWastageReason())
                        .notes(b.getNotes())
                        .createdAt(b.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private AuthModel currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthModel)) {
            throw new RuntimeException("Not authenticated");
        }
        return (AuthModel) auth.getPrincipal();
    }

    private Long resolvePcId(AuthModel user) {
        Long pcId = user.getMpcId() != null ? user.getMpcId() : user.getProductionCenterId();
        if (pcId == null) {
            throw new RuntimeException("Worker is not assigned to a production center");
        }
        return pcId;
    }

    private com.plover.backerymanagmentsystem.manager.model.RawMaterial resolveRawMaterialFromBomChild(Long childItemId, Long parentProductId) {
        if (childItemId == null) return null;
        com.plover.backerymanagmentsystem.manager.model.RawMaterial rm = rawMaterialRepository.findById(childItemId).orElse(null);
        if (rm == null) {
            var ri = recipeIngredientRepository.findById(childItemId).orElse(null);
            if (ri != null && ri.getRawMaterial() != null) {
                rm = ri.getRawMaterial();
            }
        }
        return rm;
    }

    private void deductIngredientsFromMiniStore(ProductionPlanItem item, BigDecimal producedBd, MiniStore miniStore, List<MiniStoreItem> items) {
        if (item == null || producedBd == null || producedBd.compareTo(BigDecimal.ZERO) <= 0 || miniStore == null) return;

        List<MiniStoreItem> allStoreItemsCache = null;
        java.util.Set<Long> processedProductIds = new java.util.HashSet<>();

        // 1. Deduct BOM components (Raw Materials & Sub-Assemblies/Products) using BOM ratio per unit
        if (item.getProductId() != null) {
            List<com.plover.backerymanagmentsystem.manager.model.BillOfMaterial> bomEntries = 
                    billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(item.getProductId());
            for (var bomEntry : bomEntries) {
                if (bomEntry.getQuantity() == null || bomEntry.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
                BigDecimal totalConsumed = bomEntry.getQuantity().multiply(producedBd);

                MiniStoreItem storeItem = null;
                if (bomEntry.getChildType() == com.plover.backerymanagmentsystem.manager.model.BillOfMaterial.ChildType.raw_material) {
                    Long rmId = bomEntry.getChildItemId();
                    com.plover.backerymanagmentsystem.manager.model.RawMaterial resolvedRm = rmId != null ? rawMaterialRepository.findById(rmId).orElse(null) : null;
                    String rmName = resolvedRm != null ? resolvedRm.getMaterialName() : null;
                    String genericName = resolvedRm != null ? resolvedRm.getGenericMaterialName() : null;

                    storeItem = items.stream().filter(si -> 
                            (rmId != null && rmId.equals(si.getRawMaterialId())) ||
                            (rmName != null && rmName.equalsIgnoreCase(si.getName())) ||
                            (genericName != null && genericName.equalsIgnoreCase(si.getName()))
                    ).findFirst().orElse(null);

                    if (storeItem == null) {
                        if (allStoreItemsCache == null) allStoreItemsCache = miniStoreItemRepository.findAll();
                        storeItem = allStoreItemsCache.stream().filter(si -> 
                                (rmId != null && rmId.equals(si.getRawMaterialId())) ||
                                (rmName != null && rmName.equalsIgnoreCase(si.getName())) ||
                                (genericName != null && genericName.equalsIgnoreCase(si.getName()))
                        ).findFirst().orElse(null);
                    }
                } else if (bomEntry.getChildType() == com.plover.backerymanagmentsystem.manager.model.BillOfMaterial.ChildType.product) {
                    Long childProdId = bomEntry.getChildItemId();
                    if (childProdId != null) processedProductIds.add(childProdId);
                    storeItem = items.stream().filter(si -> childProdId != null && childProdId.equals(si.getProductId())).findFirst().orElse(null);
                    if (storeItem == null) {
                        if (allStoreItemsCache == null) allStoreItemsCache = miniStoreItemRepository.findAll();
                        storeItem = allStoreItemsCache.stream().filter(si -> childProdId != null && childProdId.equals(si.getProductId())).findFirst().orElse(null);
                    }
                }

                if (storeItem != null && storeItem.getPhysicalQty() != null) {
                    BigDecimal newPhys = storeItem.getPhysicalQty().subtract(totalConsumed).max(BigDecimal.ZERO);
                    BigDecimal newSys = storeItem.getSystemQty() != null ? storeItem.getSystemQty().subtract(totalConsumed).max(BigDecimal.ZERO) : newPhys;
                    storeItem.setPhysicalQty(newPhys);
                    storeItem.setSystemQty(newSys);
                    miniStoreItemRepository.save(storeItem);
                    log.info("Deducted {} of BOM item '{}' from MiniStore #{}", totalConsumed, storeItem.getName(), storeItem.getMiniStore() != null ? storeItem.getMiniStore().getMiniStoreId() : miniStore.getMiniStoreId());
                }
            }
        }

        // 2. Deduct child plan items (sub-assemblies / components linked by parentPlanItemId) if not already deducted by BOM above
        List<ProductionPlanItem> childPlanItems = productionPlanItemRepository.findByParentPlanItemId(item.getId());
        for (ProductionPlanItem child : childPlanItems) {
            if (child.getProductId() != null && processedProductIds.contains(child.getProductId())) {
                continue; // Already processed accurately with BOM ratio above
            }

            BigDecimal childQty = child.getQuantity() != null ? BigDecimal.valueOf(child.getQuantity()) : BigDecimal.ZERO;
            if (childQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal qtyUsed = childQty;
            if (item.getQuantity() != null && item.getQuantity() > 0) {
                qtyUsed = childQty.multiply(producedBd).divide(BigDecimal.valueOf(item.getQuantity()), 4, java.math.RoundingMode.HALF_UP);
            }
            if (qtyUsed.compareTo(BigDecimal.ZERO) <= 0) continue;

            MiniStoreItem childStoreItem = null;
            if (child.getProductId() != null) {
                childStoreItem = items.stream().filter(si -> child.getProductId().equals(si.getProductId())).findFirst().orElse(null);
            }
            if (childStoreItem == null && child.getRawMaterialId() != null) {
                childStoreItem = items.stream().filter(si -> child.getRawMaterialId().equals(si.getRawMaterialId())).findFirst().orElse(null);
            }
            if (childStoreItem == null && child.getProductName() != null) {
                childStoreItem = items.stream().filter(si -> child.getProductName() != null && child.getProductName().equalsIgnoreCase(si.getName())).findFirst().orElse(null);
            }

            if (childStoreItem == null) {
                if (allStoreItemsCache == null) allStoreItemsCache = miniStoreItemRepository.findAll();
                if (child.getProductId() != null) {
                    childStoreItem = allStoreItemsCache.stream().filter(si -> child.getProductId().equals(si.getProductId())).findFirst().orElse(null);
                }
                if (childStoreItem == null && child.getRawMaterialId() != null) {
                    childStoreItem = allStoreItemsCache.stream().filter(si -> child.getRawMaterialId().equals(si.getRawMaterialId())).findFirst().orElse(null);
                }
                if (childStoreItem == null && child.getProductName() != null) {
                    childStoreItem = allStoreItemsCache.stream().filter(si -> child.getProductName() != null && child.getProductName().equalsIgnoreCase(si.getName())).findFirst().orElse(null);
                }
            }

            if (childStoreItem != null && childStoreItem.getPhysicalQty() != null) {
                BigDecimal newPhys = childStoreItem.getPhysicalQty().subtract(qtyUsed).max(BigDecimal.ZERO);
                BigDecimal newSys = childStoreItem.getSystemQty() != null ? childStoreItem.getSystemQty().subtract(qtyUsed).max(BigDecimal.ZERO) : newPhys;
                childStoreItem.setPhysicalQty(newPhys);
                childStoreItem.setSystemQty(newSys);
                miniStoreItemRepository.save(childStoreItem);
                log.info("Deducted {} of child item '{}' from MiniStore #{}", qtyUsed, childStoreItem.getName(), childStoreItem.getMiniStore() != null ? childStoreItem.getMiniStore().getMiniStoreId() : miniStore.getMiniStoreId());
            }
        }

        // 3. Deduct Recipe ingredients
        if (item.getProductId() != null) {
            var recipeOpt = recipeRepository.findByProductIdAndIsActiveTrue(item.getProductId());
            if (recipeOpt.isPresent()) {
                com.plover.backerymanagmentsystem.manager.model.Recipe recipe = recipeOpt.get();
                if (recipe.getRecipeIngredients() != null) {
                    for (com.plover.backerymanagmentsystem.manager.model.RecipeIngredient ri : recipe.getRecipeIngredients()) {
                        if (ri.getQuantityPerUnit() == null || ri.getQuantityPerUnit() <= 0) continue;
                        BigDecimal totalConsumed = BigDecimal.valueOf(ri.getQuantityPerUnit()).multiply(producedBd);

                        com.plover.backerymanagmentsystem.manager.model.RawMaterial rm = ri.getRawMaterial();
                        Long rmId = rm != null ? rm.getId() : null;
                        String rmName = rm != null ? rm.getMaterialName() : null;
                        String genericName = rm != null ? rm.getGenericMaterialName() : null;

                        MiniStoreItem matItem = items.stream().filter(si -> 
                                (rmId != null && rmId.equals(si.getRawMaterialId())) ||
                                (rmName != null && rmName.equalsIgnoreCase(si.getName())) ||
                                (genericName != null && genericName.equalsIgnoreCase(si.getName()))
                        ).findFirst().orElse(null);

                        if (matItem == null) {
                            if (allStoreItemsCache == null) allStoreItemsCache = miniStoreItemRepository.findAll();
                            matItem = allStoreItemsCache.stream().filter(si -> 
                                    (rmId != null && rmId.equals(si.getRawMaterialId())) ||
                                    (rmName != null && rmName.equalsIgnoreCase(si.getName())) ||
                                    (genericName != null && genericName.equalsIgnoreCase(si.getName()))
                            ).findFirst().orElse(null);
                        }

                        if (matItem != null && matItem.getPhysicalQty() != null) {
                            BigDecimal newPhys = matItem.getPhysicalQty().subtract(totalConsumed).max(BigDecimal.ZERO);
                            BigDecimal newSys = matItem.getSystemQty() != null ? matItem.getSystemQty().subtract(totalConsumed).max(BigDecimal.ZERO) : newPhys;
                            matItem.setPhysicalQty(newPhys);
                            matItem.setSystemQty(newSys);
                            miniStoreItemRepository.save(matItem);
                            log.info("Deducted {} of Recipe ingredient '{}' from MiniStore #{}", totalConsumed, matItem.getName(), matItem.getMiniStore() != null ? matItem.getMiniStore().getMiniStoreId() : miniStore.getMiniStoreId());
                        }
                    }
                }
            }
        }
    }
}
