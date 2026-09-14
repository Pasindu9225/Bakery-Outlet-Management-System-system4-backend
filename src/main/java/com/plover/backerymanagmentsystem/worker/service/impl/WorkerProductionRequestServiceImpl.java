package com.plover.backerymanagmentsystem.worker.service.impl;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.manager.model.ActualProduction;
import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistory;
import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistoryAction;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlan.ProductionPlanStatus;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.repository.ActualProductionHistoryRepository;
import com.plover.backerymanagmentsystem.manager.repository.ActualProductionRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanRepository;
import com.plover.backerymanagmentsystem.worker.dto.WorkerProductionRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.WorkerProductionRequestItemDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerProductionRequestService;

import com.plover.backerymanagmentsystem.manager.dto.MiniStoreItemDto;
import com.plover.backerymanagmentsystem.manager.service.MiniStoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerProductionRequestServiceImpl implements WorkerProductionRequestService {

    private final ProductionPlanRepository productionPlanRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.ProductionBatchRepository productionBatchRepository;
    private final ActualProductionRepository actualProductionRepository;
    private final ActualProductionHistoryRepository actualProductionHistoryRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final MiniStoreService miniStoreService;
    private final com.plover.backerymanagmentsystem.manager.repository.MiniStoreRepository miniStoreRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository miniStoreItemRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository billOfMaterialRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.ProductRepository productRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.IngredientRequestRepository ingredientRequestRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository productionPlanItemRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository rawMaterialRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.RecipeRepository recipeRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.RecipeIngredientRepository recipeIngredientRepository;

    @Override
    @Transactional
    public WorkerProductionRequestDto updateStatus(Long planId, String newStatus) {
        AuthModel user = currentUser();
        Long pcId = user.getProductionCenterId() != null ? user.getProductionCenterId() : user.getMpcId();

        ProductionPlanStatus parsed;
        try {
            parsed = ProductionPlanStatus.valueOf(newStatus);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid status: " + newStatus);
        }
        if (parsed != ProductionPlanStatus.IN_PROGRESS && parsed != ProductionPlanStatus.COMPLETED) {
            throw new RuntimeException("Workers can only set status IN_PROGRESS or COMPLETED");
        }

        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found with id: " + planId));

        // Check ingredient receipt gating: worker must accept/confirm ingredients in 'Get Ingredients' tab first
        List<com.plover.backerymanagmentsystem.manager.model.IngredientRequest> ingredientReqs = 
                ingredientRequestRepository.findByProductionPlanId(planId);
        if (!ingredientReqs.isEmpty()) {
            com.plover.backerymanagmentsystem.manager.model.IngredientRequest req = ingredientReqs.get(0);
            if (req.getStatus() != com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus.RECEIVED) {
                String currentStatusStr = req.getStatus() != null ? req.getStatus().name() : "PENDING";
                throw new RuntimeException("Cannot start or process production plan: You must accept/confirm receipt of required ingredients in the 'Get Ingredients' tab first (Current ingredient status: " + currentStatusStr + ").");
            }
        }

        boolean ownsItem = pcId == null || plan.getProductionPlanItems().isEmpty() || plan.getProductionPlanItems().stream()
                .anyMatch(it -> it.getProductionCenterId() == null || pcId.equals(it.getProductionCenterId()) || isItemOrChildAssignedToCenter(it, plan.getProductionPlanItems(), pcId));
        if (!ownsItem) {
            log.warn("Worker {} attempting to update plan {} status to {}", user.getUsername(), planId, parsed);
        }

        // If setting status to COMPLETED, check child completion gating, auto-fill batches, and sync to MiniStore with BOM consumption
        if (parsed == ProductionPlanStatus.COMPLETED) {
            List<ProductionPlanItem> sortedItems = plan.getProductionPlanItems().stream()
                    .sorted((i1, i2) -> {
                        boolean isTop1 = Boolean.TRUE.equals(i1.getIsTopLevel()) || i1.getParentPlanItemId() == null;
                        boolean isTop2 = Boolean.TRUE.equals(i2.getIsTopLevel()) || i2.getParentPlanItemId() == null;
                        return Boolean.compare(isTop1, isTop2); // false (children) first, true (parents) second
                    })
                    .collect(Collectors.toList());

            for (ProductionPlanItem item : sortedItems) {
                if (pcId != null && (item.getProductionCenterId() == null || pcId.equals(item.getProductionCenterId()))) {
                    // Check dependency gating for child items if this is a top-level item
                    boolean isTop = Boolean.TRUE.equals(item.getIsTopLevel()) || item.getParentPlanItemId() == null;
                    if (isTop) {
                        List<ProductionPlanItem> children = plan.getProductionPlanItems().stream()
                                .filter(c -> item.getId().equals(c.getParentPlanItemId()))
                                .collect(Collectors.toList());

                        for (ProductionPlanItem child : children) {
                            double childProduced = productionBatchRepository.findByProductionPlanItemIdOrderByCreatedAtDesc(child.getId())
                                    .stream().mapToDouble(b -> b.getExactProducedQty() != null ? b.getExactProducedQty() : (b.getProducedQty() != null ? b.getProducedQty().doubleValue() : 0.0)).sum();
                            double childTarget = child.getExactQuantity() != null && child.getExactQuantity() > 0 ? child.getExactQuantity() : (child.getQuantity() != null ? child.getQuantity().doubleValue() : 0.0);
                            if (childTarget > 0 && childProduced < childTarget - 0.0001) {
                                throw new RuntimeException("Cannot set plan status to COMPLETED: Required child item '" 
                                        + child.getProductName() + "' is not completed yet (Produced: " + childProduced + "/" + childTarget + ").");
                            }
                        }
                    }

                    double itemTarget = item.getExactQuantity() != null && item.getExactQuantity() > 0 ? item.getExactQuantity() : (item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0);
                    double produced = productionBatchRepository.findByProductionPlanItemIdOrderByCreatedAtDesc(item.getId())
                            .stream().mapToDouble(b -> b.getExactProducedQty() != null ? b.getExactProducedQty() : (b.getProducedQty() != null ? b.getProducedQty().doubleValue() : 0.0)).sum();
                    
                    if (produced <= 0.00001 && itemTarget > 0) {
                        int intQty = itemTarget < 1.0 && itemTarget > 0 ? 1 : (int) Math.round(itemTarget);
                        com.plover.backerymanagmentsystem.manager.model.ProductionBatch batch = 
                                com.plover.backerymanagmentsystem.manager.model.ProductionBatch.builder()
                                .productionPlanItemId(item.getId())
                                .producedQty(intQty)
                                .exactProducedQty(itemTarget)
                                .wastageQty(0)
                                .producedBy(user.getId())
                                .productionCenterId(pcId)
                                .build();
                        productionBatchRepository.save(batch);
                        processProductionItemCompletion(item, itemTarget, pcId);
                    }
                }
            }
        }

        plan.setStatus(parsed);
        ProductionPlan saved = productionPlanRepository.save(plan);
        log.info("Worker {} updated plan {} status to {}", user.getUsername(), planId, parsed);
        return toDto(saved, user);
    }

    @Override
    @Transactional
    public void dispatchPlan(Long planId) {
        try {
            AuthModel user = currentUser();
            ProductionPlan plan = productionPlanRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Production plan not found with id: " + planId));

            if (plan.getStatus() == ProductionPlanStatus.DRAFT || plan.getStatus() == ProductionPlanStatus.CANCELLED) {
                throw new RuntimeException("Cannot dispatch plan with status: " + plan.getStatus());
            }

            Long pcId = user.getMpcId() != null ? user.getMpcId() : user.getProductionCenterId();

            List<ProductionPlanItem> sortedItems = plan.getProductionPlanItems().stream()
                    .sorted((i1, i2) -> {
                        boolean isTop1 = Boolean.TRUE.equals(i1.getIsTopLevel()) || i1.getParentPlanItemId() == null;
                        boolean isTop2 = Boolean.TRUE.equals(i2.getIsTopLevel()) || i2.getParentPlanItemId() == null;
                        return Boolean.compare(isTop1, isTop2); // false (children) first, true (parents) second
                    })
                    .collect(Collectors.toList());

            for (ProductionPlanItem item : sortedItems) {
                if (pcId != null && item.getProductionCenterId() != null && !pcId.equals(item.getProductionCenterId())) {
                    continue;
                }
                
                boolean isTop = Boolean.TRUE.equals(item.getIsTopLevel()) || item.getParentPlanItemId() == null;

                double itemTarget = item.getExactQuantity() != null && item.getExactQuantity() > 0 ? item.getExactQuantity() : (item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0);
                double produced = productionBatchRepository.findByProductionPlanItemIdOrderByCreatedAtDesc(item.getId())
                        .stream().mapToDouble(b -> b.getExactProducedQty() != null ? b.getExactProducedQty() : (b.getProducedQty() != null ? b.getProducedQty().doubleValue() : 0.0)).sum();
                
                if (produced <= 0.00001 && itemTarget > 0) {
                    int intQty = itemTarget < 1.0 && itemTarget > 0 ? 1 : (int) Math.round(itemTarget);
                    com.plover.backerymanagmentsystem.manager.model.ProductionBatch batch = 
                            com.plover.backerymanagmentsystem.manager.model.ProductionBatch.builder()
                            .productionPlanItemId(item.getId())
                            .producedQty(intQty)
                            .exactProducedQty(itemTarget)
                            .wastageQty(0)
                            .producedBy(user.getId())
                            .productionCenterId(pcId)
                            .build();
                    productionBatchRepository.save(batch);
                    processProductionItemCompletion(item, itemTarget, pcId);
                    produced = itemTarget;
                }

                // Only top-level finished products are sent to Manager Actual Production, skip raw materials and sub-assemblies
                if (item.getProductId() == null || produced <= 0 || !isTop) {
                    continue;
                }

                // Transfer from MiniStore to Actual Production
                com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore = null;
                if (pcId != null) {
                    ProductionCenter center = productionCenterRepository.findById(pcId).orElse(null);
                    if (center != null) miniStore = center.getMiniStore();
                }
                if (miniStore != null) {
                    List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> existingStoreItems = 
                            miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStore.getMiniStoreId());
                    com.plover.backerymanagmentsystem.manager.model.MiniStoreItem storeItem = existingStoreItems.stream()
                            .filter(si -> item.getProductId().equals(si.getProductId()))
                            .findFirst().orElse(null);
                    if (storeItem != null && storeItem.getPhysicalQty() != null) {
                        java.math.BigDecimal newQty = storeItem.getPhysicalQty().subtract(java.math.BigDecimal.valueOf(produced));
                        if (newQty.compareTo(java.math.BigDecimal.ZERO) < 0) newQty = java.math.BigDecimal.ZERO;
                        storeItem.setPhysicalQty(newQty);
                        storeItem.setSystemQty(newQty);
                        miniStoreItemRepository.save(storeItem);
                    }
                }
                
                ActualProduction actualProd = actualProductionRepository.findByProductId(item.getProductId())
                        .orElseGet(() -> ActualProduction.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .availableQuantity(0)
                                .build());
                
                actualProd.setAvailableQuantity(actualProd.getAvailableQuantity() + (int) Math.round(produced));
                actualProd.setLastUpdated(OffsetDateTime.now());
                
                actualProductionRepository.save(actualProd);
                
                ActualProductionHistory history = ActualProductionHistory.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity((int) Math.round(produced))
                        .actionType(ActualProductionHistoryAction.PRODUCTION_IN)
                        .referenceName(plan.getPlanName())
                        .build();
                actualProductionHistoryRepository.save(history);
            }

            plan.setStatus(ProductionPlanStatus.DISTRIBUTED);
            productionPlanRepository.save(plan);
            log.info("Worker {} dispatched plan {}. Status updated to DISTRIBUTED.", user.getUsername(), planId);
        } catch (Exception e) {
            log.error("Error dispatching plan {}: {}", planId, e.getMessage(), e);
            throw new RuntimeException("Failed to dispatch plan: " + e.getMessage());
        }
    }

    private void processProductionItemCompletion(ProductionPlanItem item, double producedQty, Long pcId) {
        if (item == null || producedQty <= 0) return;

        // 1. Resolve ProductionCenter's MiniStore
        com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore = null;
        if (pcId != null) {
            ProductionCenter center = productionCenterRepository.findById(pcId).orElse(null);
            if (center != null) {
                miniStore = center.getMiniStore();
                if (miniStore == null) {
                    miniStore = miniStoreRepository.save(com.plover.backerymanagmentsystem.manager.model.MiniStore.builder()
                            .name(center.getCenterName() + " Store")
                            .storeDate(java.time.LocalDate.now())
                            .build());
                    center.setMiniStore(miniStore);
                    productionCenterRepository.save(center);
                }
            }
        }
        if (miniStore == null) {
            miniStore = miniStoreRepository.findAll().stream().findFirst()
                    .orElseGet(() -> miniStoreRepository.save(com.plover.backerymanagmentsystem.manager.model.MiniStore.builder()
                            .name("Production Center Store")
                            .storeDate(java.time.LocalDate.now())
                            .build()));
        }

        // 2. Add produced item to MiniStore
        List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> existingItems = 
                miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStore.getMiniStoreId());

        java.math.BigDecimal producedBd = java.math.BigDecimal.valueOf(producedQty);
        com.plover.backerymanagmentsystem.manager.model.MiniStoreItem storeItem = null;

        if (item.getProductId() != null) {
            storeItem = existingItems.stream()
                    .filter(si -> item.getProductId().equals(si.getProductId()))
                    .findFirst().orElse(null);
        }
        if (storeItem == null && item.getProductName() != null) {
            storeItem = existingItems.stream()
                    .filter(si -> item.getProductName().equalsIgnoreCase(si.getName()))
                    .findFirst().orElse(null);
        }

        if (storeItem != null) {
            java.math.BigDecimal currentPhys = storeItem.getPhysicalQty() != null ? storeItem.getPhysicalQty() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal currentSys = storeItem.getSystemQty() != null ? storeItem.getSystemQty() : java.math.BigDecimal.ZERO;
            storeItem.setPhysicalQty(currentPhys.add(producedBd));
            storeItem.setSystemQty(currentSys.add(producedBd));
            miniStoreItemRepository.save(storeItem);
        } else {
            com.plover.backerymanagmentsystem.manager.model.MiniStoreItem newItem = com.plover.backerymanagmentsystem.manager.model.MiniStoreItem.builder()
                    .name(item.getProductName() != null ? item.getProductName() : "Product #" + item.getProductId())
                    .productId(item.getProductId())
                    .systemQty(producedBd)
                    .physicalQty(producedBd)
                    .miniStore(miniStore)
                    .build();
            miniStoreItemRepository.save(newItem);
        }

        // 3. Deduct consumed child sub-assemblies, BOM components, and Recipe ingredients from MiniStore
        existingItems = miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStore.getMiniStoreId());
        deductIngredientsFromMiniStore(item, producedBd, miniStore, existingItems);
    }

    private void deductIngredientsFromMiniStore(ProductionPlanItem item, java.math.BigDecimal producedBd, com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items) {
        if (item == null || producedBd == null || producedBd.compareTo(java.math.BigDecimal.ZERO) <= 0 || miniStore == null) return;

        List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> allStoreItemsCache = null;
        java.util.Set<Long> processedProductIds = new java.util.HashSet<>();

        // 1. Deduct BOM components (Raw Materials & Sub-Assemblies/Products) using BOM ratio per unit
        if (item.getProductId() != null) {
            List<com.plover.backerymanagmentsystem.manager.model.BillOfMaterial> bomEntries = 
                    billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(item.getProductId());
            for (var bomEntry : bomEntries) {
                if (bomEntry.getQuantity() == null || bomEntry.getQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0) continue;
                java.math.BigDecimal totalConsumed = bomEntry.getQuantity().multiply(producedBd);

                com.plover.backerymanagmentsystem.manager.model.MiniStoreItem storeItem = null;
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
                    java.math.BigDecimal newPhys = storeItem.getPhysicalQty().subtract(totalConsumed).max(java.math.BigDecimal.ZERO);
                    java.math.BigDecimal newSys = storeItem.getSystemQty() != null ? storeItem.getSystemQty().subtract(totalConsumed).max(java.math.BigDecimal.ZERO) : newPhys;
                    if (newPhys.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                        miniStoreItemRepository.delete(storeItem);
                        log.info("Deleted fully consumed BOM item '{}' from MiniStore", storeItem.getName());
                    } else {
                        storeItem.setPhysicalQty(newPhys);
                        storeItem.setSystemQty(newSys);
                        miniStoreItemRepository.save(storeItem);
                        log.info("Deducted {} of BOM item '{}' from MiniStore #{}", totalConsumed, storeItem.getName(), storeItem.getMiniStore() != null ? storeItem.getMiniStore().getMiniStoreId() : miniStore.getMiniStoreId());
                    }
                }
            }
        }

        // 2. Deduct child plan items (sub-assemblies / components linked by parentPlanItemId) if not already deducted by BOM above
        List<ProductionPlanItem> childPlanItems = productionPlanItemRepository.findByParentPlanItemId(item.getId());
        for (ProductionPlanItem child : childPlanItems) {
            if (child.getProductId() != null && processedProductIds.contains(child.getProductId())) {
                continue; // Already processed accurately with BOM ratio above
            }

            java.math.BigDecimal childQty = child.getQuantity() != null ? java.math.BigDecimal.valueOf(child.getQuantity()) : java.math.BigDecimal.ZERO;
            if (childQty.compareTo(java.math.BigDecimal.ZERO) <= 0) continue;

            java.math.BigDecimal qtyUsed = childQty;
            if (item.getQuantity() != null && item.getQuantity() > 0) {
                qtyUsed = childQty.multiply(producedBd).divide(java.math.BigDecimal.valueOf(item.getQuantity()), 4, java.math.RoundingMode.HALF_UP);
            }
            if (qtyUsed.compareTo(java.math.BigDecimal.ZERO) <= 0) continue;

            com.plover.backerymanagmentsystem.manager.model.MiniStoreItem childStoreItem = null;
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
                java.math.BigDecimal newPhys = childStoreItem.getPhysicalQty().subtract(qtyUsed).max(java.math.BigDecimal.ZERO);
                java.math.BigDecimal newSys = childStoreItem.getSystemQty() != null ? childStoreItem.getSystemQty().subtract(qtyUsed).max(java.math.BigDecimal.ZERO) : newPhys;
                if (newPhys.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    miniStoreItemRepository.delete(childStoreItem);
                    log.info("Deleted fully consumed child item '{}' from MiniStore", childStoreItem.getName());
                } else {
                    childStoreItem.setPhysicalQty(newPhys);
                    childStoreItem.setSystemQty(newSys);
                    miniStoreItemRepository.save(childStoreItem);
                    log.info("Deducted {} of child item '{}' from MiniStore #{}", qtyUsed, childStoreItem.getName(), childStoreItem.getMiniStore() != null ? childStoreItem.getMiniStore().getMiniStoreId() : miniStore.getMiniStoreId());
                }
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
                        java.math.BigDecimal totalConsumed = java.math.BigDecimal.valueOf(ri.getQuantityPerUnit()).multiply(producedBd);

                        com.plover.backerymanagmentsystem.manager.model.RawMaterial rm = ri.getRawMaterial();
                        Long rmId = rm != null ? rm.getId() : null;
                        String rmName = rm != null ? rm.getMaterialName() : null;
                        String genericName = rm != null ? rm.getGenericMaterialName() : null;

                        com.plover.backerymanagmentsystem.manager.model.MiniStoreItem matItem = items.stream().filter(si -> 
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
                            java.math.BigDecimal newPhys = matItem.getPhysicalQty().subtract(totalConsumed).max(java.math.BigDecimal.ZERO);
                            java.math.BigDecimal newSys = matItem.getSystemQty() != null ? matItem.getSystemQty().subtract(totalConsumed).max(java.math.BigDecimal.ZERO) : newPhys;
                            if (newPhys.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                                miniStoreItemRepository.delete(matItem);
                                log.info("Deleted fully consumed Recipe ingredient '{}' from MiniStore", matItem.getName());
                            } else {
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

    @Override
    @Transactional
    public List<MiniStoreItemDto> getMyStoreInventory() {
        AuthModel user = currentUser();
        Long pcId = user.getMpcId() != null ? user.getMpcId() : user.getProductionCenterId();
        
        com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore = null;
        if (pcId != null) {
            ProductionCenter center = productionCenterRepository.findById(pcId).orElse(null);
            if (center != null) {
                miniStore = center.getMiniStore();
                if (miniStore == null) {
                    miniStore = miniStoreRepository.save(com.plover.backerymanagmentsystem.manager.model.MiniStore.builder()
                            .name(center.getCenterName() + " Store")
                            .storeDate(java.time.LocalDate.now())
                            .build());
                    center.setMiniStore(miniStore);
                    productionCenterRepository.save(center);
                }
            }
        }

        if (miniStore == null) {
            miniStore = miniStoreRepository.findAll().stream().findFirst()
                    .orElseGet(() -> miniStoreRepository.save(com.plover.backerymanagmentsystem.manager.model.MiniStore.builder()
                            .name("Bakery Production Center Store")
                            .storeDate(java.time.LocalDate.now())
                            .build()));
        }

        reassignMisplacedMiniStoreItems(pcId, miniStore);
        return miniStoreService.getMiniStoreItems(miniStore.getMiniStoreId());
    }

    private java.util.Set<Long> getRawMaterialIdsForCenter(Long pcId) {
        if (pcId == null) return java.util.Collections.emptySet();
        java.util.Set<Long> centerMaterialIds = new java.util.HashSet<>();
        List<com.plover.backerymanagmentsystem.manager.model.BillOfMaterial> bomList = billOfMaterialRepository.findAll();
        for (com.plover.backerymanagmentsystem.manager.model.BillOfMaterial bom : bomList) {
            if (bom.getChildType() == com.plover.backerymanagmentsystem.manager.model.BillOfMaterial.ChildType.raw_material && bom.getChildItemId() != null) {
                if (pcId.equals(bom.getProductionCenterId())) {
                    centerMaterialIds.add(bom.getChildItemId());
                }
            }
        }
        List<com.plover.backerymanagmentsystem.manager.model.Product> centerProducts = productRepository.findAll().stream()
                .filter(p -> pcId.equals(p.getProductionCenterId()))
                .collect(Collectors.toList());
        for (com.plover.backerymanagmentsystem.manager.model.Product p : centerProducts) {
            for (com.plover.backerymanagmentsystem.manager.model.BillOfMaterial bom : bomList) {
                if (p.getId().equals(bom.getParentProductId())) {
                    if (bom.getChildType() == com.plover.backerymanagmentsystem.manager.model.BillOfMaterial.ChildType.raw_material && bom.getChildItemId() != null) {
                        centerMaterialIds.add(bom.getChildItemId());
                    }
                }
            }
        }
        return centerMaterialIds;
    }

    private void reassignMisplacedMiniStoreItems(Long currentPcId, com.plover.backerymanagmentsystem.manager.model.MiniStore currentMiniStore) {
        if (currentPcId == null || currentMiniStore == null) return;
        java.util.Set<Long> myMaterialIds = getRawMaterialIdsForCenter(currentPcId);
        List<ProductionCenter> otherCenters = productionCenterRepository.findAll().stream()
                .filter(c -> !currentPcId.equals(c.getId()))
                .collect(Collectors.toList());
        if (otherCenters.isEmpty()) return;

        List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> currentStoreItems = 
                new java.util.ArrayList<>(miniStoreItemRepository.findByMiniStore_MiniStoreId(currentMiniStore.getMiniStoreId()));

        for (com.plover.backerymanagmentsystem.manager.model.MiniStoreItem item : currentStoreItems) {
            if (item.getRawMaterialId() != null && !myMaterialIds.contains(item.getRawMaterialId())) {
                for (ProductionCenter otherCenter : otherCenters) {
                    java.util.Set<Long> otherMaterialIds = getRawMaterialIdsForCenter(otherCenter.getId());
                    if (otherMaterialIds.contains(item.getRawMaterialId())) {
                        com.plover.backerymanagmentsystem.manager.model.MiniStore targetStore = otherCenter.getMiniStore();
                        if (targetStore == null) {
                            targetStore = miniStoreRepository.save(com.plover.backerymanagmentsystem.manager.model.MiniStore.builder()
                                    .name(otherCenter.getCenterName() + " Store")
                                    .storeDate(java.time.LocalDate.now())
                                    .build());
                            otherCenter.setMiniStore(targetStore);
                            productionCenterRepository.save(otherCenter);
                        }

                        List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> targetItems = 
                                miniStoreItemRepository.findByMiniStore_MiniStoreId(targetStore.getMiniStoreId());
                        com.plover.backerymanagmentsystem.manager.model.MiniStoreItem targetItem = targetItems.stream()
                                .filter(ti -> item.getRawMaterialId().equals(ti.getRawMaterialId()))
                                .findFirst().orElse(null);

                        java.math.BigDecimal qtyToMove = item.getPhysicalQty() != null ? item.getPhysicalQty() : java.math.BigDecimal.ZERO;
                        if (targetItem != null) {
                            java.math.BigDecimal curPhys = targetItem.getPhysicalQty() != null ? targetItem.getPhysicalQty() : java.math.BigDecimal.ZERO;
                            java.math.BigDecimal curSys = targetItem.getSystemQty() != null ? targetItem.getSystemQty() : java.math.BigDecimal.ZERO;
                            targetItem.setPhysicalQty(curPhys.add(qtyToMove));
                            targetItem.setSystemQty(curSys.add(qtyToMove));
                            miniStoreItemRepository.save(targetItem);
                            miniStoreItemRepository.delete(item);
                        } else {
                            item.setMiniStore(targetStore);
                            miniStoreItemRepository.save(item);
                        }
                        log.info("Reassigned misplaced mini store item '{}' (rawMaterialId: {}) from centerId {} to centerId {}",
                                item.getName(), item.getRawMaterialId(), currentPcId, otherCenter.getId());
                        break;
                    }
                }
            }
        }
    }

    private void syncReceivedRequestsToMiniStore(Long pcId, com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore) {
        if (miniStore == null) return;

        List<com.plover.backerymanagmentsystem.manager.model.IngredientRequest> receivedRequests = 
                ingredientRequestRepository.findByStatusOrderByCreatedAtAsc(com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus.RECEIVED);
        
        if (pcId != null) {
            receivedRequests = receivedRequests.stream()
                    .filter(req -> pcId.equals(req.getProductionCenterId()))
                    .collect(Collectors.toList());
        }

        List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> existingStoreItems = 
                miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStore.getMiniStoreId());

        for (var req : receivedRequests) {
            if (req.getItems() == null) continue;
            for (var reqItem : req.getItems()) {
                Double val = reqItem.getIssuedQty() != null && reqItem.getIssuedQty() > 0 
                        ? reqItem.getIssuedQty() 
                        : reqItem.getRequestedQty();
                java.math.BigDecimal qtyToAdd = val != null ? java.math.BigDecimal.valueOf(val) : java.math.BigDecimal.ZERO;

                if (qtyToAdd == null || qtyToAdd.compareTo(java.math.BigDecimal.ZERO) <= 0) continue;

                com.plover.backerymanagmentsystem.manager.model.MiniStoreItem storeItem = null;
                if (reqItem.getRawMaterialId() != null) {
                    storeItem = existingStoreItems.stream()
                            .filter(si -> reqItem.getRawMaterialId().equals(si.getRawMaterialId()))
                            .findFirst().orElse(null);
                }
                if (storeItem == null && reqItem.getRawMaterialName() != null) {
                    storeItem = existingStoreItems.stream()
                            .filter(si -> reqItem.getRawMaterialName() != null && reqItem.getRawMaterialName().equalsIgnoreCase(si.getName()))
                            .findFirst().orElse(null);
                }

                if (storeItem != null) {
                    if (storeItem.getSystemQty() == null) {
                        storeItem.setPhysicalQty(qtyToAdd);
                        storeItem.setSystemQty(qtyToAdd);
                        miniStoreItemRepository.save(storeItem);
                    }
                } else {
                    com.plover.backerymanagmentsystem.manager.model.MiniStoreItem newItem = com.plover.backerymanagmentsystem.manager.model.MiniStoreItem.builder()
                            .name(reqItem.getRawMaterialName() != null ? reqItem.getRawMaterialName() : "Material #" + reqItem.getRawMaterialId())
                            .rawMaterialId(reqItem.getRawMaterialId())
                            .systemQty(qtyToAdd)
                            .physicalQty(qtyToAdd)
                            .miniStore(miniStore)
                            .build();
                    com.plover.backerymanagmentsystem.manager.model.MiniStoreItem savedItem = miniStoreItemRepository.save(newItem);
                    existingStoreItems.add(savedItem);
                }
            }
        }
    }

    @Override
    public List<WorkerProductionRequestDto> listMyRequests() {
        AuthModel user = currentUser();
        Long pcId = user.getProductionCenterId() != null ? user.getProductionCenterId() : user.getMpcId();
        log.info("[WorkerProductionRequestService] listMyRequests for user: {}, pcId: {}", user.getUsername(), pcId);
        List<ProductionPlan> plans = productionPlanRepository.findActivePlansByProductionCenterId(pcId);
        log.info("[WorkerProductionRequestService] Found {} active plans for pcId: {}", plans.size(), pcId);
        return plans.stream().map(p -> toDto(p, user)).collect(Collectors.toList());
    }

    private AuthModel currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthModel)) {
            throw new RuntimeException("Not authenticated");
        }
        return (AuthModel) auth.getPrincipal();
    }

    private WorkerProductionRequestDto toDto(ProductionPlan p, AuthModel user) {
        Long pcId = user != null ? (user.getProductionCenterId() != null ? user.getProductionCenterId() : user.getMpcId()) : null;
        Map<Long, String> centerNames = productionCenterRepository.findAll().stream()
                .collect(Collectors.toMap(ProductionCenter::getId, ProductionCenter::getCenterName, (a, b) -> a));

        List<ProductionPlanItem> allPlanItems = p.getProductionPlanItems() != null ? p.getProductionPlanItems() : List.of();

        // Get top-level items (parentPlanItemId == null OR parent item not found in plan)
        List<WorkerProductionRequestItemDto> topLevelDtos = allPlanItems.stream()
                .filter(it -> (it.getParentPlanItemId() == null || allPlanItems.stream().noneMatch(parent -> parent.getId().equals(it.getParentPlanItemId()))))
                .map(it -> buildItemTreeDto(it, allPlanItems, pcId, centerNames, user))
                .collect(Collectors.toList());

        log.debug("[WorkerProductionRequestService] Plan #{} has {} top-level items for user pcId: {}", p.getId(), topLevelDtos.size(), pcId);

        int total = topLevelDtos.stream().mapToInt(it -> it.getQuantity() != null ? it.getQuantity() : 0).sum();
        int totalProduced = topLevelDtos.stream().mapToInt(it -> it.getProducedQuantity() != null ? it.getProducedQuantity() : 0).sum();

        boolean ingredientsConfirmed = true;
        String ingredientRequestStatus = null;
        if (p.getId() != null) {
            List<com.plover.backerymanagmentsystem.manager.model.IngredientRequest> reqs = 
                    ingredientRequestRepository.findByProductionPlanId(p.getId());
            if (!reqs.isEmpty()) {
                com.plover.backerymanagmentsystem.manager.model.IngredientRequest req = reqs.get(0);
                ingredientRequestStatus = req.getStatus() != null ? req.getStatus().name() : "PENDING";
                ingredientsConfirmed = (req.getStatus() == com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus.RECEIVED);
            }
        }

        return WorkerProductionRequestDto.builder()
                .id(p.getId())
                .planName(p.getPlanName())
                .planDate(p.getPlanDate())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .department(p.getDepartment())
                .notes(p.getNotes())
                .items(topLevelDtos)
                .totalQuantity(total)
                .producedQuantity(totalProduced)
                .ingredientsConfirmed(ingredientsConfirmed)
                .ingredientRequestStatus(ingredientRequestStatus)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private boolean isItemOrChildAssignedToCenter(ProductionPlanItem item, List<ProductionPlanItem> allItems, Long pcId) {
        if (pcId == null || item.getProductionCenterId() == null || pcId.equals(item.getProductionCenterId())) {
            return true;
        }
        return allItems.stream()
                .filter(child -> item.getId().equals(child.getParentPlanItemId()))
                .anyMatch(child -> isItemOrChildAssignedToCenter(child, allItems, pcId));
    }

    private WorkerProductionRequestItemDto buildItemTreeDto(ProductionPlanItem item, List<ProductionPlanItem> allItems, Long pcId, Map<Long, String> centerNames, AuthModel user) {
        double exactProd = productionBatchRepository.findByProductionPlanItemIdOrderByCreatedAtDesc(item.getId())
                .stream().mapToDouble(b -> b.getExactProducedQty() != null ? b.getExactProducedQty() : (b.getProducedQty() != null ? b.getProducedQty().doubleValue() : 0.0)).sum();
        int produced = (int) Math.round(exactProd);

        double targetQty = item.getExactQuantity() != null && item.getExactQuantity() > 0 ? item.getExactQuantity() : (item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0);
        boolean miniStoreFulfilled = Boolean.TRUE.equals(item.getMiniStoreFulfilled());
        boolean isCompleted = (targetQty > 0 && exactProd >= targetQty - 0.0001) || miniStoreFulfilled;

        Long primaryPcId = user != null && user.getProductionCenterId() != null ? user.getProductionCenterId() : pcId;
        Long mpcId = user != null ? user.getMpcId() : null;

        String centerName = item.getProductionCenterId() != null ? centerNames.get(item.getProductionCenterId()) : null;
        String userCenterName = primaryPcId != null ? centerNames.get(primaryPcId) : null;
        if (userCenterName == null && user != null && user.getProductionCenterId() != null) {
            userCenterName = productionCenterRepository.findById(user.getProductionCenterId())
                    .map(ProductionCenter::getCenterName).orElse(null);
        }

        boolean isSameCenterByName = (userCenterName != null && centerName != null 
                && userCenterName.trim().equalsIgnoreCase(centerName.trim()));

        boolean isLocalCenter = (primaryPcId == null 
                || item.getProductionCenterId() == null 
                || item.getProductionCenterId().equals(primaryPcId)
                || (mpcId != null && item.getProductionCenterId().equals(mpcId))
                || isSameCenterByName);

        List<WorkerProductionRequestItemDto> childDtos = allItems.stream()
                .filter(child -> item.getId().equals(child.getParentPlanItemId()))
                .map(child -> buildItemTreeDto(child, allItems, pcId, centerNames, user))
                .collect(Collectors.toList());

        int uncompletedCount = childDtos.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsCompleted()))
                .mapToInt(c -> 1 + (c.getUncompletedChildCount() != null ? c.getUncompletedChildCount() : 0))
                .sum();

        boolean isBlocked = uncompletedCount > 0;
        boolean canProduceLocally = isLocalCenter && (!isBlocked || childDtos.isEmpty());

        String unitStr = null;
        if (item.getProductId() != null) {
            unitStr = productRepository.findById(item.getProductId())
                    .map(com.plover.backerymanagmentsystem.manager.model.Product::getUnitOfMeasure)
                    .orElse("kg");
        }

        return WorkerProductionRequestItemDto.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .productId(item.getProductId())
                .rawMaterialId(item.getRawMaterialId())
                .quantity(item.getQuantity())
                .exactQuantity(targetQty)
                .unit(unitStr)
                .unitOfMeasure(unitStr)
                .producedQuantity(produced)
                .exactProducedQuantity(exactProd)
                .productionCenterId(item.getProductionCenterId())
                .productionCenterName(centerName)
                .parentPlanItemId(item.getParentPlanItemId())
                .isLocalCenter(isLocalCenter)
                .isCompleted(isCompleted)
                .canProduceLocally(canProduceLocally)
                .isBlocked(isBlocked)
                .uncompletedChildCount(uncompletedCount)
                .reservedFromMiniStore(item.getReservedFromMiniStore())
                .miniStoreFulfilled(miniStoreFulfilled)
                .children(childDtos)
                .build();
    }

    @Override
    @Transactional
    public void clearProductionCenterMiniStore() {
        List<ProductionCenter> centers = productionCenterRepository.findAll();
        for (ProductionCenter center : centers) {
            String name = center.getCenterName() != null ? center.getCenterName().toLowerCase() : "";
            if (name.contains("kitchen") || name.contains("bakery")) {
                com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore = center.getMiniStore();
                if (miniStore != null && miniStore.getMiniStoreId() != null) {
                    List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = 
                            miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStore.getMiniStoreId());
                    if (!items.isEmpty()) {
                        miniStoreItemRepository.deleteAll(items);
                        log.info("Cleared {} items from mini store #{} for center '{}'", items.size(), miniStore.getMiniStoreId(), center.getCenterName());
                    }
                }
            }
        }
        List<com.plover.backerymanagmentsystem.manager.model.MiniStore> allStores = miniStoreRepository.findAll();
        for (com.plover.backerymanagmentsystem.manager.model.MiniStore store : allStores) {
            String sName = store.getName() != null ? store.getName().toLowerCase() : "";
            if (sName.contains("kitchen") || sName.contains("bakery")) {
                List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = 
                        miniStoreItemRepository.findByMiniStore_MiniStoreId(store.getMiniStoreId());
                if (!items.isEmpty()) {
                    miniStoreItemRepository.deleteAll(items);
                    log.info("Cleared {} items from mini store #{} ('{}')", items.size(), store.getMiniStoreId(), store.getName());
                }
            }
        }
    }
}
