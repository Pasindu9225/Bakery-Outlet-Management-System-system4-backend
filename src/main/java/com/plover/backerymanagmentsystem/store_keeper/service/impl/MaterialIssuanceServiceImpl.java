package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.ProductionOrder;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanMaterials;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.model.RawMaterialRequirement;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanMaterialsRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.InsufficientStockException;
import com.plover.backerymanagmentsystem.store_keeper.exception.MaterialsAlreadyIssuedException;
import com.plover.backerymanagmentsystem.store_keeper.exception.ProductionPlanNotApprovedException;
import com.plover.backerymanagmentsystem.store_keeper.exception.ProductionPlanNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.repository.ProductionOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialRequirementRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.StoreKeeperProductionPlanRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.MaterialIssuanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of MaterialIssuanceService. Handles material issuance with
 * proper validation and atomic operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MaterialIssuanceServiceImpl implements MaterialIssuanceService {

    private final StoreKeeperProductionPlanRepository productionPlanRepository;
    private final ProductionPlanMaterialsRepository productionPlanMaterialsRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final RawMaterialRequirementRepository rawMaterialRequirementRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.IngredientRequestRepository ingredientRequestRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.MiniStoreRepository miniStoreRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository miniStoreItemRepository;

    private com.plover.backerymanagmentsystem.manager.model.MiniStore resolveMiniStore(Integer miniStoreId) {
        if (miniStoreId != null) {
            java.util.Optional<com.plover.backerymanagmentsystem.manager.model.MiniStore> storeOpt = miniStoreRepository.findById(miniStoreId);
            if (storeOpt.isPresent()) {
                return storeOpt.get();
            }
        }
        java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> allStores = miniStoreRepository.findAll();
        if (!allStores.isEmpty()) {
            return allStores.get(0);
        }
        com.plover.backerymanagmentsystem.manager.model.MiniStore defaultStore = com.plover.backerymanagmentsystem.manager.model.MiniStore.builder()
                .name("Main Mini Store")
                .storeDate(java.time.LocalDate.now())
                .build();
        return miniStoreRepository.save(defaultStore);
    }

    @Override
    public IssueMaterialsResponseDto issueMaterialsForProductionPlan(com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto requestDto) {
        log.info("Starting material issuance (manual) for production plan ID: {}", requestDto.getProductionPlanId());

        // Step 1: Validate production plan exists and is approved
        ProductionPlan productionPlan = validateProductionPlan(requestDto.getProductionPlanId());

        // Step 2: Check if materials already issued
        validateMaterialsNotAlreadyIssued(requestDto.getProductionPlanId());

        // Step 3-4: Validate sufficient stock for each material across all available batches
        java.util.Map<String, Double> totalRequestedPerCode = new java.util.HashMap<>();
        for (com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto.MiniStoreMaterialsDto group : requestDto.getMiniStoreMaterials()) {
            resolveMiniStore(group.getMiniStoreId());
            for (com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto.IssuedMaterialDto m : group.getIssuedMaterials()) {
                if (m.getIssuedQty() == null || m.getIssuedQty().signum() <= 0) {
                    throw new IllegalArgumentException("Issued quantity must be positive for material: " + m.getMaterialName());
                }
                
                RawMaterial rawMaterial = rawMaterialRepository.findById(m.getRawMaterialId())
                        .orElseThrow(() -> new IllegalArgumentException("Raw material not found: " + m.getRawMaterialId()));
                
                String code = rawMaterial.getMaterialCode();
                String key = (code != null) ? "CODE:" + code : "ID:" + rawMaterial.getId();
                totalRequestedPerCode.merge(key, m.getIssuedQty().doubleValue(), Double::sum);
            }
        }

        java.util.List<String> insufficientStockDetails = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Double> entry : totalRequestedPerCode.entrySet()) {
            String key = entry.getKey();
            Double requiredQty = entry.getValue();
            Double totalAvailable = 0.0;
            String materialName = "";
            String code = "";

            if (key.startsWith("CODE:")) {
                code = key.substring(5);
                String rootCode = code.contains(" - ") ? code.split(" - ")[0].trim() : code;
                java.util.List<RawMaterial> batches = rawMaterialRepository.findByMaterialCodeStartingWith(rootCode);
                if (batches == null || batches.isEmpty()) {
                    batches = rawMaterialRepository.findAllByMaterialCode(code);
                }
                totalAvailable = batches.stream().mapToDouble(RawMaterial::getCurrentStock).sum();
                if (!batches.isEmpty()) materialName = batches.get(0).getMaterialName();
            } else {
                Long id = Long.parseLong(key.substring(3));
                RawMaterial rm = rawMaterialRepository.findById(id).orElse(null);
                if (rm != null) {
                    totalAvailable = rm.getCurrentStock();
                    materialName = rm.getMaterialName();
                    code = rm.getMaterialCode();
                }
            }

            if (totalAvailable < requiredQty) {
                String displayMaterialName = (materialName != null && !materialName.isBlank()) ? materialName : "Raw Material";
                String label = (code != null && !code.isBlank()) ? displayMaterialName + " [" + code + "]" : displayMaterialName;
                insufficientStockDetails.add(String.format("%s (Required: %.2f, Available Total: %.2f)", 
                        label, requiredQty, totalAvailable));
            }
        }

        if (!insufficientStockDetails.isEmpty()) {
            throw new com.plover.backerymanagmentsystem.store_keeper.exception.InsufficientStockException(insufficientStockDetails);
        }

        // Step 5: Create or get production order
        ProductionOrder productionOrder = createOrGetProductionOrder(productionPlan);

        // Step 6: Issue materials based on request and update mini store(s)
        java.util.List<IssueMaterialsResponseDto.IssuedMaterialDetail> issuedDetails = new java.util.ArrayList<>();

        for (com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto.MiniStoreMaterialsDto group : requestDto.getMiniStoreMaterials()) {
            com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore = resolveMiniStore(group.getMiniStoreId());

            for (com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto.IssuedMaterialDto m : group.getIssuedMaterials()) {
                RawMaterial requestedMaterial = rawMaterialRepository.findById(m.getRawMaterialId())
                        .orElseThrow(() -> new IllegalArgumentException("Raw material not found: " + m.getRawMaterialId()));

                java.math.BigDecimal remainingToIssue = m.getIssuedQty();
                
                // Collect all possible batches matching materialCode or rootCode prefix
                java.util.List<RawMaterial> availableBatches = new java.util.ArrayList<>();
                availableBatches.add(requestedMaterial);
                
                String materialCode = requestedMaterial.getMaterialCode();
                if (materialCode != null) {
                    String rootCode = materialCode.contains(" - ") ? materialCode.split(" - ")[0].trim() : materialCode;
                    java.util.List<RawMaterial> otherBatches = rawMaterialRepository.findByMaterialCodeStartingWith(rootCode);
                    if (otherBatches == null || otherBatches.isEmpty()) {
                        otherBatches = rawMaterialRepository.findAllByMaterialCode(materialCode);
                    }
                    for (RawMaterial other : otherBatches) {
                        if (!other.getId().equals(requestedMaterial.getId()) && other.getCurrentStock() > 0) {
                            availableBatches.add(other);
                        }
                    }
                }

                for (RawMaterial batch : availableBatches) {
                    if (remainingToIssue.compareTo(java.math.BigDecimal.ZERO) <= 0) break;
                    
                    Double batchStock = batch.getCurrentStock();
                    if (batchStock <= 0) continue;
                    
                    java.math.BigDecimal issueFromThisBatch = remainingToIssue.min(java.math.BigDecimal.valueOf(batchStock));
                    Double previousStock = batchStock;
                    Double updatedStock = previousStock - issueFromThisBatch.doubleValue();

                    // Deduct stock
                    batch.setCurrentStock(updatedStock);
                    rawMaterialRepository.save(batch);

                    // Create raw material requirement record
                    Double unitCost = m.getUnitCost() != null ? m.getUnitCost().doubleValue() : batch.getUnitCost();
                    createRawMaterialRequirement(productionOrder, batch, issueFromThisBatch.doubleValue(), previousStock);

                    // Note: Mini store items are upserted when the worker confirms receipt in the Worker module.

                    // Build response detail
                    IssueMaterialsResponseDto.IssuedMaterialDetail detail = IssueMaterialsResponseDto.IssuedMaterialDetail.builder()
                            .materialId(batch.getId())
                            .materialName(batch.getMaterialName())
                            .materialCode(batch.getMaterialCode())
                            .issuedQuantity(issueFromThisBatch.doubleValue())
                            .unitOfMeasure(batch.getUnitOfMeasure())
                            .previousStock(previousStock)
                            .updatedStock(updatedStock)
                            .unitCost(unitCost)
                            .totalCost(issueFromThisBatch.doubleValue() * unitCost)
                            .miniStoreId(miniStore.getMiniStoreId())
                            .build();

                    issuedDetails.add(detail);
                    
                    remainingToIssue = remainingToIssue.subtract(issueFromThisBatch);
                }
            }
        }

        // Step 7: Update production plan status & linked ingredient requests
        updateProductionPlanStatus(productionPlan);
        updateLinkedIngredientRequests(productionPlan.getId());

        // Step 8: Build and return response
        IssueMaterialsResponseDto response = buildSuccessResponse(productionPlan, issuedDetails);
        log.info("Successfully issued materials (manual) for production plan ID: {}", requestDto.getProductionPlanId());
        return response;
    }

    private ProductionPlan validateProductionPlan(Long planId) {
        ProductionPlan productionPlan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new ProductionPlanNotFoundException(planId));

        if (!ProductionPlan.ProductionPlanStatus.APPROVED.equals(productionPlan.getStatus())) {
            throw new ProductionPlanNotApprovedException(planId, productionPlan.getStatus().toString());
        }

        log.debug("Production plan validation successful for ID: {}", planId);
        return productionPlan;
    }

    private void validateMaterialsNotAlreadyIssued(Long planId) {
        boolean alreadyIssued = rawMaterialRequirementRepository.existsByProductionOrder_ProductionPlan_Id(planId);
        if (alreadyIssued) {
            throw new MaterialsAlreadyIssuedException(planId);
        }

        log.debug("Materials not yet issued for production plan ID: {}", planId);
    }

    private List<ProductionPlanMaterials> getRequiredMaterials(Long planId) {
        List<ProductionPlanMaterials> materials = productionPlanMaterialsRepository.findByProductionPlan_Id(planId);

        if (materials.isEmpty()) {
            log.warn("No materials found for production plan ID: {}", planId);
        }

        log.debug("Found {} materials for production plan ID: {}", materials.size(), planId);
        return materials;
    }

    private void validateSufficientStock(List<ProductionPlanMaterials> requiredMaterials) {
        List<String> insufficientMaterials = new ArrayList<>();

        for (ProductionPlanMaterials material : requiredMaterials) {
            RawMaterial rawMaterial = material.getRawMaterial();
            Double requiredQuantity = material.getRawMaterialQuantity();
            Double availableStock = 0.0;

            String code = rawMaterial.getMaterialCode();
            if (code != null && !code.isBlank()) {
                String rootCode = code.contains(" - ") ? code.split(" - ")[0].trim() : code;
                List<RawMaterial> batches = rawMaterialRepository.findByMaterialCodeStartingWith(rootCode);
                if (batches == null || batches.isEmpty()) {
                    batches = rawMaterialRepository.findAllByMaterialCode(code);
                }
                availableStock = batches.stream().mapToDouble(RawMaterial::getCurrentStock).sum();
            } else {
                availableStock = rawMaterial.getCurrentStock();
            }

            if (availableStock < requiredQuantity) {
                String label = (code != null && !code.isBlank()) ? rawMaterial.getMaterialName() + " [" + code + "]" : rawMaterial.getMaterialName();
                String insufficientDetail = String.format("%s (Required: %.2f, Available Total: %.2f)",
                        label, requiredQuantity, availableStock);
                insufficientMaterials.add(insufficientDetail);
            }
        }

        if (!insufficientMaterials.isEmpty()) {
            log.error("Insufficient stock for materials: {}", insufficientMaterials);
            throw new InsufficientStockException(insufficientMaterials);
        }

        log.debug("Stock validation successful for all materials");
    }

    private List<IssueMaterialsResponseDto.IssuedMaterialDetail> issueMaterials(
            List<ProductionPlanMaterials> requiredMaterials, ProductionPlan productionPlan) {

        List<IssueMaterialsResponseDto.IssuedMaterialDetail> issuedDetails = new ArrayList<>();

        // Create a production order if it doesn't exist
        ProductionOrder productionOrder = createOrGetProductionOrder(productionPlan);

        for (ProductionPlanMaterials material : requiredMaterials) {
            RawMaterial rawMaterial = material.getRawMaterial();
            Double issuedQuantity = material.getRawMaterialQuantity();
            Double previousStock = rawMaterial.getCurrentStock();
            Double updatedStock = previousStock - issuedQuantity;

            // Update raw material stock
            rawMaterial.setCurrentStock(updatedStock);
            rawMaterialRepository.save(rawMaterial);

            // Create raw material requirement record
            createRawMaterialRequirement(productionOrder, rawMaterial, issuedQuantity, previousStock);

            // Build issued material detail for response
            IssueMaterialsResponseDto.IssuedMaterialDetail detail = IssueMaterialsResponseDto.IssuedMaterialDetail.builder()
                    .materialId(rawMaterial.getId())
                    .materialName(rawMaterial.getMaterialName())
                    .materialCode(rawMaterial.getMaterialCode())
                    .issuedQuantity(issuedQuantity)
                    .unitOfMeasure(rawMaterial.getUnitOfMeasure())
                    .previousStock(previousStock)
                    .updatedStock(updatedStock)
                    .unitCost(rawMaterial.getUnitCost())
                    .totalCost(issuedQuantity * rawMaterial.getUnitCost())
                    .build();

            issuedDetails.add(detail);

            log.debug("Issued {} {} of {} (Previous stock: {}, Updated stock: {})",
                    issuedQuantity, rawMaterial.getUnitOfMeasure(), rawMaterial.getMaterialName(),
                    previousStock, updatedStock);
        }

        return issuedDetails;
    }

    private ProductionOrder createOrGetProductionOrder(ProductionPlan productionPlan) {
        // Check if production order already exists for this plan
        List<ProductionOrder> existingOrders = productionOrderRepository.findByProductionPlan_Id(productionPlan.getId());

        if (!existingOrders.isEmpty()) {
            log.debug("Using existing production order for plan ID: {}", productionPlan.getId());
            return existingOrders.get(0);
        }

        // Create new production order
        ProductionOrder productionOrder = ProductionOrder.builder()
                .productionPlan(productionPlan)
                .orderNumber("PO-" + productionPlan.getId() + "-" + System.currentTimeMillis())
                .orderDate(LocalDateTime.now())
                .status(ProductionOrder.ProductionOrderStatus.IN_PROGRESS)
                .build();

        ProductionOrder savedOrder = productionOrderRepository.save(productionOrder);
        log.debug("Created new production order for plan ID: {}", productionPlan.getId());
        return savedOrder;
    }

    private void createRawMaterialRequirement(ProductionOrder productionOrder, RawMaterial rawMaterial,
            Double issuedQuantity, Double availableStock) {

        Double stockDeficit = Math.max(0, issuedQuantity - availableStock);

        RawMaterialRequirement requirement = RawMaterialRequirement.builder()
                .productionOrder(productionOrder)
                .rawMaterial(rawMaterial)
                .requiredQuantity(issuedQuantity)
                .unitOfMeasure(rawMaterial.getUnitOfMeasure())
                .unitCost(rawMaterial.getUnitCost())
                .totalCost(issuedQuantity * rawMaterial.getUnitCost())
                .availableStock(availableStock)
                .stockDeficit(stockDeficit)
                .build();

        rawMaterialRequirementRepository.save(requirement);

        log.debug("Created raw material requirement record for material: {}", rawMaterial.getMaterialName());
    }

    private void updateProductionPlanStatus(ProductionPlan productionPlan) {
        productionPlan.setStatus(ProductionPlan.ProductionPlanStatus.IN_PROGRESS);
        productionPlanRepository.save(productionPlan);

        log.debug("Updated production plan status to IN_PROGRESS for ID: {}", productionPlan.getId());
    }

    private void updateLinkedIngredientRequests(Long planId) {
        List<com.plover.backerymanagmentsystem.manager.model.IngredientRequest> linkedRequests = ingredientRequestRepository.findByProductionPlanId(planId);
        for (com.plover.backerymanagmentsystem.manager.model.IngredientRequest req : linkedRequests) {
            if (req.getStatus() == com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus.PENDING) {
                req.setStatus(com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus.ISSUED);
                req.setIssuedAt(LocalDateTime.now());
                if (req.getItems() != null) {
                    for (com.plover.backerymanagmentsystem.manager.model.IngredientRequestItem item : req.getItems()) {
                        if (item.getIssuedQty() == null || item.getIssuedQty() <= 0) {
                            item.setIssuedQty(item.getRequestedQty());
                        }
                    }
                }
                ingredientRequestRepository.save(req);
                log.info("Updated linked IngredientRequest ID {} to ISSUED for plan ID {}", req.getId(), planId);
            }
        }
    }

    private IssueMaterialsResponseDto buildSuccessResponse(ProductionPlan productionPlan,
            List<IssueMaterialsResponseDto.IssuedMaterialDetail> issuedDetails) {

        return IssueMaterialsResponseDto.builder()
                .message("Materials successfully issued for production plan")
                .productionPlanId(productionPlan.getId())
                .productionPlanName(productionPlan.getPlanName())
                .issuanceDateTime(LocalDateTime.now())
                .issuedMaterials(issuedDetails)
                .build();
    }
}
