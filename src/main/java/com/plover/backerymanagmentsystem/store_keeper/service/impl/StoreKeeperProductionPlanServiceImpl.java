package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanMaterials;
import com.plover.backerymanagmentsystem.manager.model.Recipe;
import com.plover.backerymanagmentsystem.manager.model.RecipeIngredient;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanMaterialsRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.MiniStoreAvailabilityDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductSummary;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionCenterSummaryDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionPlanMaterialSummaryResponse;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionPlanMaterialSummaryResponse.ProductionPlanMaterialDetails;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialRequirementsResponse;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialRequirementsResponse.RawMaterialRequirementDTO;
import com.plover.backerymanagmentsystem.store_keeper.dto.EnhancedProductionPlanSummaryResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionPlanSummaryResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.repository.StoreKeeperProductionPlanRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionCenterResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.service.MaterialIssuanceService;
import com.plover.backerymanagmentsystem.store_keeper.service.StoreKeeperProductionPlanService;
import com.plover.backerymanagmentsystem.store_keeper.service.BomService;
import com.plover.backerymanagmentsystem.store_keeper.dto.BomTreeResponseDto;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.PackDetailsRepository;
import com.plover.backerymanagmentsystem.manager.model.PackDetails;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;

/**
 * Service implementation for Store Keeper's Production Plan operations. Handles
 * the retrieval and processing of approved production plans.
 *
 */
import com.plover.backerymanagmentsystem.store_keeper.service.GrnService;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialReturnService;
import com.plover.backerymanagmentsystem.store_keeper.service.StockAdjustmentService;
import com.plover.backerymanagmentsystem.manager.repository.IngredientRequestRepository;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreKeeperProductionPlanServiceImpl implements StoreKeeperProductionPlanService {

    private final StoreKeeperProductionPlanRepository storeKeeperProductionPlanRepository;
    private final ProductionPlanMaterialsRepository productionPlanMaterialsRepository;
    private final ProductionPlanItemRepository productionPlanItemRepository;
    private final ProductRepository productRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final RecipeRepository recipeRepository;
    private final MaterialIssuanceService materialIssuanceService;
    private final com.plover.backerymanagmentsystem.manager.repository.MiniStoreRepository miniStoreRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository miniStoreItemRepository;
    private final BomService bomService;
    private final BillOfMaterialRepository billOfMaterialRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final PackDetailsRepository packDetailsRepository;
    private final GrnService grnService;
    private final RawMaterialReturnService rawMaterialReturnService;
    private final StockAdjustmentService stockAdjustmentService;
    private final IngredientRequestRepository ingredientRequestRepository;
    private final com.plover.backerymanagmentsystem.store_keeper.service.LowStockMaterialsService lowStockMaterialsService;

    private com.plover.backerymanagmentsystem.manager.model.MiniStore getOrCreateDefaultMiniStore() {
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
    public List<ProductionCenterResponseDto> getAllProductionCenters() {
        log.info("Fetching all production centers from database");
        com.plover.backerymanagmentsystem.manager.model.MiniStore defaultStore = getOrCreateDefaultMiniStore();
        return productionCenterRepository.findActiveAndEstablished().stream()
                .map(center -> ProductionCenterResponseDto.builder()
                        .id(center.getId())
                        .centerName(center.getCenterName())
                        .miniStoreId(center.getMiniStore() != null ? center.getMiniStore().getMiniStoreId() : defaultStore.getMiniStoreId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public ProductionPlanMaterialSummaryResponse getProductionPlanWiseMaterialSummary() {
        List<ProductionPlan.ProductionPlanStatus> activePlanStatuses = List.of(
                ProductionPlan.ProductionPlanStatus.APPROVED,
                ProductionPlan.ProductionPlanStatus.IN_PROGRESS,
                ProductionPlan.ProductionPlanStatus.COMPLETED,
                ProductionPlan.ProductionPlanStatus.DISTRIBUTED
        );
        List<ProductionPlan> approvedPlans = storeKeeperProductionPlanRepository.findByStatusIn(activePlanStatuses);

        List<ProductionPlanMaterialDetails> planDetails = approvedPlans.stream()
                .map(plan -> {
                    List<ProductionPlanItem> planItems = productionPlanItemRepository.findByProductionPlan_Id(plan.getId());

                    // Preload mini stores list once per plan
                    java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores = miniStoreRepository.findAll();

                    // Get all production centers from database
                    Map<Long, String> allCenterNames = productionCenterRepository.findAll().stream()
                            .collect(Collectors.toMap(ProductionCenter::getId, ProductionCenter::getCenterName));

                    // Group products by production center
                    Map<Long, List<ProductSummary>> productsByCenter = new HashMap<>();

                    // Process each product item and get its raw materials from recipe
                    List<ProductSummary> allProducts = planItems.stream()
                            .map(planItem -> {
                                // Get product details
                                Product product = productRepository.findById(planItem.getProductId()).orElse(null);

                                // Get production center from product
                                Long productionCenterId = null;
                                String productionCenterName = null;
                                if (product != null && product.getProductionCenterId() != null) {
                                    productionCenterId = product.getProductionCenterId();
                                    productionCenterName = allCenterNames.get(productionCenterId);
                                }

                                // Determine if product needs recipe (category '3' doesn't need recipe)
                                boolean needsRecipe = true;
                                String category = null;
                                if (product != null && product.getCategoryRef() != null) {
                                    category = product.getCategoryRef().getName();
                                    needsRecipe = !"3".equals(category);
                                }

                                // Build mini store availability for this product
                                java.util.List<MiniStoreAvailabilityDto> productAvailability = miniStores.stream().map(store -> {
                                    java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = miniStoreItemRepository.findByMiniStore_MiniStoreId(store.getMiniStoreId());
                                    java.math.BigDecimal qty = items.stream()
                                            .filter(i -> i.getProductId() != null && i.getProductId().equals(planItem.getProductId()))
                                            .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                                            .filter(java.util.Objects::nonNull)
                                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                                    return MiniStoreAvailabilityDto.builder()
                                            .miniStoreId(store.getMiniStoreId())
                                            .miniStoreName(store.getName())
                                            .availableQty(qty)
                                            .build();
                                }).collect(Collectors.toList());

                                return ProductSummary.builder()
                                        .productId(planItem.getProductId())
                                        .productName(planItem.getProductName())
                                        .quantity(planItem.getQuantity())
                                        .unitCost(planItem.getUnitCost())
                                        .totalCost(planItem.getTotalCost())
                                        .estimatedRawMaterialCost(planItem.getEstimatedRawMaterialCost())
                                        .productionCenterName(productionCenterName)
                                        .productionCenterId(productionCenterId)
                                        .needsRecipe(needsRecipe)
                                        .miniStoreAvailability(productAvailability)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    // Group products by production center
                    for (ProductSummary product : allProducts) {
                        if (product.getProductionCenterId() != null) {
                            productsByCenter.computeIfAbsent(product.getProductionCenterId(), k -> new ArrayList<>()).add(product);
                        }
                    }

                    // Create production center summaries with products and their raw materials
                    List<ProductionCenterSummaryDto> productionCenterSummaries = new ArrayList<>();

                    for (Map.Entry<Long, List<ProductSummary>> entry : productsByCenter.entrySet()) {
                        Long centerId = entry.getKey();
                        String centerName = allCenterNames.get(centerId);
                        List<ProductSummary> products = entry.getValue();

                        // Get raw materials for all products in this center
                        List<ProductionPlanMaterialSummaryResponse.MaterialQuantitySummary> centerMaterials = new ArrayList<>();
                        Map<Long, ProductionPlanMaterialSummaryResponse.MaterialQuantitySummary> materialMap = new HashMap<>();

                        for (ProductSummary product : products) {
                            double effQty = product.getExactQuantity() != null && product.getExactQuantity() > 0 
                                    ? product.getExactQuantity() 
                                    : (product.getQuantity() != null ? product.getQuantity().doubleValue() : 0.0);

                            Map<Long, MaterialQtyCost> prodMaterials = resolveRawMaterialsForProduct(product.getProductId(), effQty, new java.util.HashSet<>());

                            for (Map.Entry<Long, MaterialQtyCost> matEntry : prodMaterials.entrySet()) {
                                Long materialId = matEntry.getKey();
                                MaterialQtyCost mqc = matEntry.getValue();
                                Double totalQuantity = mqc.qty;
                                Double unitCost = mqc.rawMaterial.getUnitCost() != null ? mqc.rawMaterial.getUnitCost() : 0.0;
                                Double totalCost = totalQuantity * unitCost;

                                // Build mini store availability for this raw material
                                java.util.List<MiniStoreAvailabilityDto> materialAvailability = miniStores.stream().map(store -> {
                                    java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = miniStoreItemRepository.findByMiniStore_MiniStoreId(store.getMiniStoreId());
                                    java.math.BigDecimal qty = items.stream()
                                            .filter(i -> i.getRawMaterialId() != null && i.getRawMaterialId().equals(materialId))
                                            .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                                            .filter(java.util.Objects::nonNull)
                                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                                    return MiniStoreAvailabilityDto.builder()
                                            .miniStoreId(store.getMiniStoreId())
                                            .miniStoreName(store.getName())
                                            .availableQty(qty)
                                            .build();
                                }).collect(Collectors.toList());

                                // Accumulate quantities for this material
                                ProductionPlanMaterialSummaryResponse.MaterialQuantitySummary existing = materialMap.get(materialId);
                                if (existing == null) {
                                    existing = ProductionPlanMaterialSummaryResponse.MaterialQuantitySummary.builder()
                                            .materialId(materialId)
                                            .materialName(mqc.rawMaterial.getDisplayName())
                                            .unitMeasure(mqc.rawMaterial.getUnitOfMeasure())
                                            .kitchenQuantity(0.0)
                                            .bakeryQuantity(0.0)
                                            .totalQuantity(0.0)
                                            .totalCost(0.0)
                                            .expireDate(mqc.rawMaterial.getExpireDate())
                                            .miniStoreAvailability(materialAvailability)
                                            .build();
                                    materialMap.put(materialId, existing);
                                }

                                // Add quantities based on production center
                                if (centerName != null && centerName.toLowerCase().contains("kitchen")) {
                                    existing.setKitchenQuantity(existing.getKitchenQuantity() + totalQuantity);
                                } else if (centerName != null && centerName.toLowerCase().contains("bakery")) {
                                    existing.setBakeryQuantity(existing.getBakeryQuantity() + totalQuantity);
                                }
                                existing.setTotalQuantity(existing.getTotalQuantity() + totalQuantity);
                                existing.setTotalCost(existing.getTotalCost() + totalCost);
                            }
                        }

                        centerMaterials.addAll(materialMap.values());

                        // Calculate total cost for this center
                        double centerCost = centerMaterials.stream()
                                .mapToDouble(m -> m.getTotalCost() != null ? m.getTotalCost() : 0.0)
                                .sum();
                        if (centerCost <= 0.0) {
                            centerCost = products.stream()
                                    .mapToDouble(ProductSummary::getTotalCost)
                                    .sum();
                        }

                        productionCenterSummaries.add(ProductionCenterSummaryDto.builder()
                                .productionCenterId(centerId)
                                .productionCenterName(centerName)
                                .materials(centerMaterials)
                                .products(products)
                                    .totalCost(centerCost)
                                    .build());
                    }

                    return ProductionPlanMaterialDetails.builder()
                            .productionPlanId(plan.getId())
                            .planName(plan.getPlanName())
                            .planDate(plan.getPlanDate())
                            .createdAt(plan.getCreatedAt())
                            .updatedAt(plan.getUpdatedAt())
                            .status(plan.getStatus())
                            .productionCenterSummaries(productionCenterSummaries)
                            .totalPlanCost(plan.getTotalEstimatedCost())
                            .build();
                })
                .collect(Collectors.toList());

        return ProductionPlanMaterialSummaryResponse.builder()
                .productionPlanSummaries(planDetails)
                .build();
    }

    @Override
    public RawMaterialRequirementsResponse getApprovedProductionPlanMaterials() {
        // Get all approved production plans
        List<ProductionPlan.ProductionPlanStatus> activePlanStatuses = List.of(
                ProductionPlan.ProductionPlanStatus.APPROVED,
                ProductionPlan.ProductionPlanStatus.IN_PROGRESS,
                ProductionPlan.ProductionPlanStatus.COMPLETED,
                ProductionPlan.ProductionPlanStatus.DISTRIBUTED
        );
        List<ProductionPlan> approvedPlans = storeKeeperProductionPlanRepository.findByStatusIn(activePlanStatuses);

        // Pre-load all raw materials grouped by material code for batch details
        List<RawMaterial> allRawMaterialsInDb = rawMaterialRepository.findAll();
        Map<String, List<RawMaterial>> rawMaterialBatchMap = allRawMaterialsInDb.stream()
                .filter(rm -> rm.getMaterialCode() != null)
                .collect(Collectors.groupingBy(RawMaterial::getMaterialCode));

        // Map to store aggregated material requirements (Key: materialCode or "ID:" + materialId)
        Map<String, RawMaterialRequirementDTO> materialMap = new HashMap<>();

        // Process each approved plan
        for (ProductionPlan plan : approvedPlans) {
            List<ProductionPlanMaterials> materials = productionPlanMaterialsRepository.findByProductionPlan_Id(plan.getId());

            // Aggregate materials
            for (ProductionPlanMaterials material : materials) {
                RawMaterial rm = material.getRawMaterial();
                String code = rm.getMaterialCode();
                String key = (code != null) ? code : "ID:" + rm.getId();

                materialMap.compute(key, (k, existing) -> {
                    if (existing == null) {
                        // First occurrence of this material
                        RawMaterialRequirementDTO dto = RawMaterialRequirementDTO.builder()
                                .materialId(rm.getId())
                                .materialName(rm.getDisplayName())
                                .materialCode(code)
                                .totalQuantity(material.getRawMaterialQuantity())
                                .totalCost(rm.getUnitCost() * material.getRawMaterialQuantity())
                                .bakeryQuantity(Objects.requireNonNullElse(material.getRawMaterialQuantityForBakery(), 0.0))
                                .kitchenQuantity(Objects.requireNonNullElse(material.getRawMaterialQuantityForKitchen(), 0.0))
                                .unitOfMeasure(rm.getUnitOfMeasure())
                                .build();

                        // Populate batches list
                        List<RawMaterialRequirementsResponse.BatchRequirementDTO> batchDtos = new ArrayList<>();
                        if (code != null) {
                            List<RawMaterial> batchList = rawMaterialBatchMap.getOrDefault(code, new ArrayList<>());
                            batchDtos = batchList.stream()
                                    .map(batch -> RawMaterialRequirementsResponse.BatchRequirementDTO.builder()
                                            .id(batch.getId())
                                            .batchNo(batch.getBatchNo())
                                            .currentStock(batch.getCurrentStock())
                                            .expireDate(batch.getExpireDate())
                                            .unitCost(batch.getUnitCost())
                                            .build())
                                    .collect(Collectors.toList());
                        } else {
                            // If no code, treat as a single batch
                            batchDtos.add(RawMaterialRequirementsResponse.BatchRequirementDTO.builder()
                                    .id(rm.getId())
                                    .batchNo(rm.getBatchNo())
                                    .currentStock(rm.getCurrentStock())
                                    .expireDate(rm.getExpireDate())
                                    .unitCost(rm.getUnitCost())
                                    .build());
                        }
                        dto.setBatches(batchDtos);
                        return dto;
                    } else {
                        // Update existing totals
                        existing.setTotalQuantity(existing.getTotalQuantity() + material.getRawMaterialQuantity());
                        existing.setTotalCost(existing.getTotalCost() + (rm.getUnitCost() * material.getRawMaterialQuantity()));

                        // Update production center quantities if they exist
                        if (material.getRawMaterialQuantityForBakery() != null) {
                            existing.setBakeryQuantity(existing.getBakeryQuantity() + material.getRawMaterialQuantityForBakery());
                        }
                        if (material.getRawMaterialQuantityForKitchen() != null) {
                            existing.setKitchenQuantity(existing.getKitchenQuantity() + material.getRawMaterialQuantityForKitchen());
                        }
                        return existing;
                    }
                });
            }
        }

        return RawMaterialRequirementsResponse.builder()
                .rawMaterialRequirements(new ArrayList<>(materialMap.values()))
                .build();
    }

    @Override
    public IssueMaterialsResponseDto issueMaterialsForApprovedPlans(com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto requestDto) {
        return materialIssuanceService.issueMaterialsForProductionPlan(requestDto);
    }

    @Override
    public EnhancedProductionPlanSummaryResponseDto getEnhancedProductionPlanSummary() {
        List<ProductionPlan.ProductionPlanStatus> activePlanStatuses = List.of(
                ProductionPlan.ProductionPlanStatus.APPROVED,
                ProductionPlan.ProductionPlanStatus.IN_PROGRESS,
                ProductionPlan.ProductionPlanStatus.COMPLETED,
                ProductionPlan.ProductionPlanStatus.DISTRIBUTED
        );
        List<ProductionPlan> approvedPlans = storeKeeperProductionPlanRepository.findByStatusIn(activePlanStatuses);
        
        // Pre-load all reference data to avoid N+1 queries
        Map<Long, Product> productMap = loadAllProducts();
        Map<Long, RawMaterial> rawMaterialMap = loadAllRawMaterials();
        Map<Long, ProductionCenter> productionCenterMap = loadAllProductionCenters();
        Map<Long, List<BillOfMaterial>> bomMap = loadAllBoms();
        Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap = loadAllMiniStoreItems();
        Map<Long, String> productionCenterNamesMap = productionCenterMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getCenterName()));
        
        List<EnhancedProductionPlanSummaryResponseDto.ProductionPlanWithBomDto> productionPlansWithBom = approvedPlans.stream()
                .map(plan -> buildProductionPlanWithBomOptimized(plan, productMap, rawMaterialMap, productionCenterMap, bomMap, miniStoreMap, productionCenterNamesMap))
                .collect(Collectors.toList());
        
        return EnhancedProductionPlanSummaryResponseDto.builder()
                .productionPlans(productionPlansWithBom)
                .build();
    }

    @Override
    public ProductionPlanSummaryResponseDto getProductionPlanSummary() {
        List<ProductionPlan.ProductionPlanStatus> activePlanStatuses = List.of(
                ProductionPlan.ProductionPlanStatus.APPROVED,
                ProductionPlan.ProductionPlanStatus.IN_PROGRESS,
                ProductionPlan.ProductionPlanStatus.COMPLETED,
                ProductionPlan.ProductionPlanStatus.DISTRIBUTED
        );
        List<ProductionPlan> approvedPlans = storeKeeperProductionPlanRepository.findByStatusIn(activePlanStatuses);
        
        if (approvedPlans.isEmpty()) {
            return ProductionPlanSummaryResponseDto.builder()
                    .productionPlans(new ArrayList<>())
                    .build();
        }
        
        // Pre-load all reference data to avoid N+1 queries
        Map<Long, Product> productMap = loadAllProducts();
        Map<Long, RawMaterial> rawMaterialMap = loadAllRawMaterials();
        Map<Long, ProductionCenter> productionCenterMap = loadAllProductionCenters();
        Map<Long, List<BillOfMaterial>> bomMap = loadAllBoms();
        Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap = loadAllMiniStoreItems();
        java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores = miniStoreRepository.findAll();
        
        Map<Long, String> productionCenterNamesMap = productionCenterMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getCenterName()));
        
        // Pre-load all raw materials grouped by material code for batch details
        List<RawMaterial> allRawMaterialsInDb = rawMaterialRepository.findAll();
        Map<String, List<RawMaterial>> rawMaterialBatchMap = allRawMaterialsInDb.stream()
                .filter(rm -> rm.getMaterialCode() != null)
                .collect(Collectors.groupingBy(RawMaterial::getMaterialCode));

        // Pre-load pack UOMs for any raw material with unit "pack"
        Map<Long, String> packUomMap = loadAllPackUoms();
        
        // Build individual production plans
        List<ProductionPlanSummaryResponseDto.ProductionPlanDto> productionPlanDtos = approvedPlans.stream()
                .map(plan -> buildIndividualProductionPlan(plan, productMap, rawMaterialMap, rawMaterialBatchMap, productionCenterNamesMap, bomMap, miniStoreMap, miniStores, packUomMap))
                .collect(Collectors.toList());
        
        return ProductionPlanSummaryResponseDto.builder()
                .productionPlans(productionPlanDtos)
                .build();
    }

    private EnhancedProductionPlanSummaryResponseDto.ProductionPlanWithBomDto buildProductionPlanWithBom(ProductionPlan plan) {
        List<ProductionPlanItem> planItems = productionPlanItemRepository.findByProductionPlan_Id(plan.getId());
        
        List<EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto> productsWithBom = planItems.stream()
                    .map(item -> {
                    // Check if this is a raw material item (product_id is null, raw_material_id is not null)
                    if (item.getProductId() == null && item.getRawMaterialId() != null) {
                        // Handle raw material directly
                        return buildRawMaterialItem(item);
                    } else {
                        // Handle product with BOM
                        return buildProductItem(item);
                    }
                })
                .filter(Objects::nonNull) // Remove null items (e.g., if raw material not found)
                    .collect(Collectors.toList());
        
        return EnhancedProductionPlanSummaryResponseDto.ProductionPlanWithBomDto.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .productionDate(plan.getPlanDate())
                .createdBy("Manager") // You can get this from user context
                .remarks(plan.getNotes())
                .totalEstimatedCost(plan.getTotalEstimatedCost())
                .status(plan.getStatus() != null ? plan.getStatus().name() : null)
                .products(productsWithBom)
                .build();
    }

    /**
     * Builds a raw material item directly without BOM processing
     */
    private EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto buildRawMaterialItem(ProductionPlanItem item) {
        // Get raw material details
        RawMaterial rawMaterial = rawMaterialRepository.findById(item.getRawMaterialId()).orElse(null);
        if (rawMaterial == null) {
            return null; // Skip if raw material not found
        }
        
        // Get production center name from the item's production center
        String productionCenterName = "Unknown";
        if (item.getProductionCenterId() != null) {
            ProductionCenter productionCenter = productionCenterRepository.findById(item.getProductionCenterId()).orElse(null);
            if (productionCenter != null) {
                productionCenterName = productionCenter.getCenterName();
            }
        }
        
        // Build mini store availability for this raw material
        java.util.List<MiniStoreAvailabilityDto> miniStoreAvailability = miniStoreRepository.findAll().stream().map(store -> {
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = miniStoreItemRepository.findByMiniStore_MiniStoreId(store.getMiniStoreId());
            java.math.BigDecimal qty = items.stream()
                    .filter(i -> i.getRawMaterialId() != null && i.getRawMaterialId().equals(item.getRawMaterialId()))
                    .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            return MiniStoreAvailabilityDto.builder()
                    .miniStoreId(store.getMiniStoreId())
                    .miniStoreName(store.getName())
                    .availableQty(qty)
                    .build();
        }).collect(Collectors.toList());
        
        // Calculate total active stock across all batches with the same material code
        double totalStock = 0.0;
        if (rawMaterial.getMaterialCode() != null && !rawMaterial.getMaterialCode().trim().isEmpty()) {
            List<RawMaterial> batches = rawMaterialRepository.findAllByMaterialCode(rawMaterial.getMaterialCode());
            totalStock = batches.stream()
                    .filter(b -> b.getIsActive() != null && b.getIsActive())
                    .mapToDouble(b -> b.getCurrentStock() != null ? b.getCurrentStock() : 0.0)
                    .sum();
        } else {
            totalStock = rawMaterial.getCurrentStock() != null ? rawMaterial.getCurrentStock() : 0.0;
        }

        // Create a single BOM node for the raw material
        EnhancedProductionPlanSummaryResponseDto.BomNodeDto rawMaterialNode = EnhancedProductionPlanSummaryResponseDto.BomNodeDto.builder()
                .childItemId(rawMaterial.getId())
                .childName(rawMaterial.getMaterialName())
                .childType("raw_material")
                .quantity(BigDecimal.valueOf(item.getQuantity()))
                .unit(rawMaterial.getUnitOfMeasure())
                .unitCost(BigDecimal.valueOf(rawMaterial.getUnitCost()))
                .totalCost(BigDecimal.valueOf(rawMaterial.getUnitCost()).multiply(BigDecimal.valueOf(item.getQuantity())))
                .productionCenter(productionCenterName)
                .miniStoreAvailability(miniStoreAvailability)
                .expireDate(rawMaterial.getExpireDate())
                .currentStock(BigDecimal.valueOf(totalStock))
                .children(new ArrayList<>()) // Raw materials don't have children
                .build();
        
        return EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto.builder()
                .productId(null) // No product ID for raw materials
                .productName(item.getProductName()) // Use the product name from the item
                .plannedQuantity(item.getQuantity())
                .productionCenter(productionCenterName)
                .children(List.of(rawMaterialNode)) // Single raw material node
                .build();
    }
    
    /**
     * Builds a product item with BOM processing
     */
    private EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto buildProductItem(ProductionPlanItem item) {
                        // Get BOM tree for this product
                        BomTreeResponseDto bomTree = bomService.getBomTree(item.getProductId());
                        
        // Scale quantities and add costs
                        List<EnhancedProductionPlanSummaryResponseDto.BomNodeDto> scaledChildren = 
            scaleBomTreeQuantities(bomTree.getChildren(), BigDecimal.valueOf(item.getQuantity()));
                        
        // Get production center name for this product from the products table
                        String productionCenterName = "Unknown";
        Product product = productRepository.findById(item.getProductId()).orElse(null);
                        if (product != null && product.getProductionCenterId() != null) {
            ProductionCenter productionCenter = productionCenterRepository.findById(product.getProductionCenterId()).orElse(null);
                            if (productionCenter != null) {
                                productionCenterName = productionCenter.getCenterName();
                            }
                        }
                        
                        return EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .plannedQuantity(item.getQuantity())
                                .productionCenter(productionCenterName)
                                .children(scaledChildren)
                                .build();
    }

    private List<EnhancedProductionPlanSummaryResponseDto.BomNodeDto> scaleBomTreeQuantities(
            List<BomTreeResponseDto.BomNodeDto> bomNodes, BigDecimal currentMultiplier) {
        
        return bomNodes.stream().map(node -> {
            // Scale the quantity using the accumulated parent multiplier
            BigDecimal scaledQuantity = node.getQuantity().multiply(currentMultiplier);
            
            // Calculate costs for raw materials and get expire date/current stock
            BigDecimal unitCost = null;
            BigDecimal totalCost = null;
            java.time.LocalDate expireDate = null;
            BigDecimal currentStock = null;
            
            if ("raw_material".equals(node.getChildType())) {
                // Fetch raw material to get unit cost, expire date, and current stock
                RawMaterial rawMaterial = rawMaterialRepository.findById(node.getChildItemId()).orElse(null);
                if (rawMaterial != null) {
                    unitCost = BigDecimal.valueOf(rawMaterial.getUnitCost());
                    totalCost = scaledQuantity.multiply(unitCost);
                    expireDate = rawMaterial.getExpireDate();
                    
                    double nodeTotalStock = 0.0;
                    if (rawMaterial.getMaterialCode() != null && !rawMaterial.getMaterialCode().trim().isEmpty()) {
                        List<RawMaterial> batches = rawMaterialRepository.findAllByMaterialCode(rawMaterial.getMaterialCode());
                        nodeTotalStock = batches.stream()
                                .filter(b -> b.getIsActive() != null && b.getIsActive())
                                .mapToDouble(b -> b.getCurrentStock() != null ? b.getCurrentStock() : 0.0)
                                .sum();
                    } else {
                        nodeTotalStock = rawMaterial.getCurrentStock() != null ? rawMaterial.getCurrentStock() : 0.0;
                    }
                    currentStock = BigDecimal.valueOf(nodeTotalStock);
                } else {
                    log.warn("Raw material not found for childItemId: {} in scaleBomTreeQuantities", node.getChildItemId());
                }
            }
            
            // Build mini store availability for this node (product or raw material)
            java.util.List<MiniStoreAvailabilityDto> miniStoreAvailability = miniStoreRepository.findAll().stream().map(store -> {
                java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = miniStoreItemRepository.findByMiniStore_MiniStoreId(store.getMiniStoreId());
                java.math.BigDecimal qty = items.stream()
                        .filter(i -> ("product".equals(node.getChildType()) && i.getProductId() != null && i.getProductId().equals(node.getChildItemId()))
                                || ("raw_material".equals(node.getChildType()) && i.getRawMaterialId() != null && i.getRawMaterialId().equals(node.getChildItemId())))
                        .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                        .filter(java.util.Objects::nonNull)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            return MiniStoreAvailabilityDto.builder()
                    .miniStoreId(store.getMiniStoreId())
                    .miniStoreName(store.getName())
                    .availableQty(qty)
                    .build();
            }).collect(Collectors.toList());
            
            // Recursively scale children passing the node's scaledQuantity as multiplier
            List<EnhancedProductionPlanSummaryResponseDto.BomNodeDto> scaledChildren = 
                node.getChildren() != null ? scaleBomTreeQuantities(node.getChildren(), scaledQuantity) : new ArrayList<>();
            
            return EnhancedProductionPlanSummaryResponseDto.BomNodeDto.builder()
                    .childItemId(node.getChildItemId())
                    .childName(node.getChildName())
                    .childType(node.getChildType())
                    .quantity(scaledQuantity)
                    .unit(node.getUnit())
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .productionCenter(node.getProductionCenter())
                    .miniStoreAvailability(miniStoreAvailability)
                    .expireDate(expireDate)
                    .currentStock(currentStock)
                    .children(scaledChildren)
                    .build();
        }).collect(Collectors.toList());
    }

    // ========== OPTIMIZATION METHODS ==========
    
    /**
     * Pre-loads all products to avoid N+1 queries
     */
    private Map<Long, Product> loadAllProducts() {
        return productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }
    
    /**
     * Pre-loads all raw materials to avoid N+1 queries
     */
    private Map<Long, RawMaterial> loadAllRawMaterials() {
        List<RawMaterial> allRawMaterials = rawMaterialRepository.findAll();
        log.info("Loaded {} raw materials from database", allRawMaterials.size());
        Map<Long, RawMaterial> rawMaterialMap = allRawMaterials.stream()
                .collect(Collectors.toMap(RawMaterial::getId, rawMaterial -> rawMaterial));
        log.debug("Raw material map keys: {}", rawMaterialMap.keySet());
        return rawMaterialMap;
    }
    
    /**
     * Pre-loads all production centers to avoid N+1 queries
     */
    private Map<Long, ProductionCenter> loadAllProductionCenters() {
        return productionCenterRepository.findAll().stream()
                .collect(Collectors.toMap(ProductionCenter::getId, productionCenter -> productionCenter));
    }
    
    /**
     * Pre-loads all BOMs grouped by parent product ID to avoid recursive queries
     */
    private Map<Long, List<BillOfMaterial>> loadAllBoms() {
        List<BillOfMaterial> allBoms = billOfMaterialRepository.findAll();
        List<BillOfMaterial> activeBoms = allBoms.stream()
                .filter(bom -> bom.getIsActive() != null && bom.getIsActive())
                .collect(Collectors.toList());
        log.info("Loaded {} active BOMs from database", activeBoms.size());
        
        // Log raw material references
        List<BillOfMaterial> rawMaterialBoms = activeBoms.stream()
                .filter(bom -> bom.getChildType() == BillOfMaterial.ChildType.raw_material)
                .collect(Collectors.toList());
        log.debug("Raw material BOM references: {}", rawMaterialBoms.stream()
                .map(bom -> "childItemId=" + bom.getChildItemId() + ", parentProductId=" + bom.getParentProductId())
                .collect(Collectors.toList()));
        
        return activeBoms.stream()
                .collect(Collectors.groupingBy(BillOfMaterial::getParentProductId));
    }
    
    /**
     * Pre-loads all mini store items grouped by product/raw material ID
     */
    private Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> loadAllMiniStoreItems() {
        return miniStoreItemRepository.findAll().stream()
                .collect(Collectors.groupingBy(item -> 
                    item.getProductId() != null ? item.getProductId() : item.getRawMaterialId()));
    }
    
    /**
     * Optimized version of buildProductionPlanWithBom that uses pre-loaded data
     */
    private EnhancedProductionPlanSummaryResponseDto.ProductionPlanWithBomDto buildProductionPlanWithBomOptimized(
            ProductionPlan plan,
            Map<Long, Product> productMap,
            Map<Long, RawMaterial> rawMaterialMap,
            Map<Long, ProductionCenter> productionCenterMap,
            Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            Map<Long, String> productionCenterNamesMap) {
        
        List<ProductionPlanItem> planItems = productionPlanItemRepository.findByProductionPlan_Id(plan.getId());
        
        List<EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto> productsWithBom = planItems.stream()
                .map(item -> {
                    // Check if this is a raw material item (product_id is null, raw_material_id is not null)
                    if (item.getProductId() == null && item.getRawMaterialId() != null) {
                        // Handle raw material directly
                        return buildRawMaterialItemOptimized(item, rawMaterialMap, productionCenterMap, productionCenterNamesMap, miniStoreMap);
                    } else {
                        // Handle product with BOM
                        return buildProductItemOptimized(item, productMap, productionCenterMap, productionCenterNamesMap, bomMap, rawMaterialMap, miniStoreMap);
                    }
                })
                .filter(Objects::nonNull) // Remove null items (e.g., if raw material not found)
                .collect(Collectors.toList());
        
        return EnhancedProductionPlanSummaryResponseDto.ProductionPlanWithBomDto.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .productionDate(plan.getPlanDate())
                .createdBy("Manager") // You can get this from user context
                .remarks(plan.getNotes())
                .totalEstimatedCost(plan.getTotalEstimatedCost())
                .status(plan.getStatus() != null ? plan.getStatus().name() : null)
                .products(productsWithBom)
                .build();
    }
    
    /**
     * Optimized version of buildRawMaterialItem that uses pre-loaded data
     */
    private EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto buildRawMaterialItemOptimized(
            ProductionPlanItem item,
            Map<Long, RawMaterial> rawMaterialMap,
            Map<Long, ProductionCenter> productionCenterMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap) {
        
        // Get raw material details from cache
        RawMaterial rawMaterial = rawMaterialMap.get(item.getRawMaterialId());
        if (rawMaterial == null) {
            return null; // Skip if raw material not found
        }
        
        // Get production center name from cache
        String productionCenterName = productionCenterNamesMap.getOrDefault(item.getProductionCenterId(), "Unknown");
        
        // Build mini store availability for this raw material using cached data
        java.util.List<MiniStoreAvailabilityDto> miniStoreAvailability = miniStoreRepository.findAll().stream().map(store -> {
            List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = miniStoreMap.getOrDefault(item.getRawMaterialId(), new ArrayList<>());
            java.math.BigDecimal qty = items.stream()
                    .filter(i -> i.getRawMaterialId() != null && i.getRawMaterialId().equals(item.getRawMaterialId()))
                    .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            return MiniStoreAvailabilityDto.builder()
                    .miniStoreId(store.getMiniStoreId())
                    .miniStoreName(store.getName())
                    .availableQty(qty)
                    .build();
        }).collect(Collectors.toList());
        
        // Create a single BOM node for the raw material
        EnhancedProductionPlanSummaryResponseDto.BomNodeDto rawMaterialNode = EnhancedProductionPlanSummaryResponseDto.BomNodeDto.builder()
                .childItemId(rawMaterial.getId())
                .childName(rawMaterial.getDisplayName())
                .childType("raw_material")
                .quantity(BigDecimal.valueOf(item.getQuantity()))
                .unit(rawMaterial.getUnitOfMeasure())
                .unitCost(BigDecimal.valueOf(rawMaterial.getUnitCost()))
                .totalCost(BigDecimal.valueOf(rawMaterial.getUnitCost()).multiply(BigDecimal.valueOf(item.getQuantity())))
                .productionCenter(productionCenterName)
                .miniStoreAvailability(miniStoreAvailability)
                .expireDate(rawMaterial.getExpireDate())
                .currentStock(BigDecimal.valueOf(rawMaterial.getCurrentStock()))
                .children(new ArrayList<>()) // Raw materials don't have children
                .build();
        
        return EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto.builder()
                .productId(null) // No product ID for raw materials
                .productName(item.getProductName()) // Use the product name from the item
                .plannedQuantity(item.getQuantity())
                .productionCenter(productionCenterName)
                .children(List.of(rawMaterialNode)) // Single raw material node
                .build();
    }
    
    /**
     * Optimized version of buildProductItem that uses pre-loaded data
     */
    private EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto buildProductItemOptimized(
            ProductionPlanItem item,
            Map<Long, Product> productMap,
            Map<Long, ProductionCenter> productionCenterMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, RawMaterial> rawMaterialMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap) {
        
        // Get BOM tree for this product using cached data
        List<EnhancedProductionPlanSummaryResponseDto.BomNodeDto> scaledChildren = 
            buildBomTreeOptimized(item.getProductId(), BigDecimal.valueOf(item.getQuantity()), bomMap, productMap, rawMaterialMap, productionCenterNamesMap, miniStoreMap);
        
        // Get production center name for this product from cache
        Product product = productMap.get(item.getProductId());
        String productionCenterName = "Unknown";
        if (product != null && product.getProductionCenterId() != null) {
            productionCenterName = productionCenterNamesMap.getOrDefault(product.getProductionCenterId(), "Unknown");
        }
        
        return EnhancedProductionPlanSummaryResponseDto.ProductWithBomDto.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .plannedQuantity(item.getQuantity())
                .productionCenter(productionCenterName)
                .children(scaledChildren)
                .build();
    }
    
    /**
     * Optimized BOM tree building using pre-loaded data
     */
    private List<EnhancedProductionPlanSummaryResponseDto.BomNodeDto> buildBomTreeOptimized(
            Long parentProductId,
            BigDecimal currentMultiplier,
            Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, Product> productMap,
            Map<Long, RawMaterial> rawMaterialMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap) {
        
        List<BillOfMaterial> bomRows = bomMap.getOrDefault(parentProductId, new ArrayList<>());
        
        if (bomRows.isEmpty()) {
            Optional<Recipe> recipeOpt = recipeRepository.findByProductIdAndIsActiveTrue(parentProductId);
            if (recipeOpt.isPresent() && recipeOpt.get().getRecipeIngredients() != null) {
                Recipe recipe = recipeOpt.get();
                Product p = productMap.get(parentProductId);
                String pcName = p != null && p.getProductionCenterId() != null ? productionCenterNamesMap.getOrDefault(p.getProductionCenterId(), "Unknown") : "Unknown";
                
                return recipe.getRecipeIngredients().stream().map(ri -> {
                    RawMaterial rm = ri.getRawMaterial();
                    Long rmId = rm != null ? rm.getId() : null;
                    String rmName = rm != null ? rm.getDisplayName() : "Unknown";
                    BigDecimal scaledQuantity = BigDecimal.valueOf(ri.getQuantityPerUnit() != null ? ri.getQuantityPerUnit() : 0.0).multiply(currentMultiplier);
                    BigDecimal unitCost = rm != null && rm.getUnitCost() != null ? BigDecimal.valueOf(rm.getUnitCost()) : BigDecimal.ZERO;
                    BigDecimal totalCost = scaledQuantity.multiply(unitCost);
                    
                    java.util.List<MiniStoreAvailabilityDto> miniStoreAvailability = miniStoreRepository.findAll().stream().map(store -> {
                        List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = miniStoreMap.getOrDefault(rmId, new ArrayList<>());
                        java.math.BigDecimal qty = items.stream()
                                .filter(i -> i.getRawMaterialId() != null && i.getRawMaterialId().equals(rmId))
                                .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                                .filter(java.util.Objects::nonNull)
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                        return MiniStoreAvailabilityDto.builder()
                                .miniStoreId(store.getMiniStoreId())
                                .miniStoreName(store.getName())
                                .availableQty(qty)
                                .build();
                    }).collect(Collectors.toList());
                    
                    return EnhancedProductionPlanSummaryResponseDto.BomNodeDto.builder()
                            .childItemId(rmId)
                            .childName(rmName)
                            .childType("raw_material")
                            .quantity(scaledQuantity)
                            .unit(ri.getUnitOfMeasure() != null ? ri.getUnitOfMeasure() : (rm != null ? rm.getUnitOfMeasure() : "kg"))
                            .unitCost(unitCost)
                            .totalCost(totalCost)
                            .productionCenter(pcName)
                            .miniStoreAvailability(miniStoreAvailability)
                            .expireDate(rm != null ? rm.getExpireDate() : null)
                            .currentStock(rm != null ? BigDecimal.valueOf(getAggregatedCurrentStock(rm)) : BigDecimal.ZERO)
                            .children(new ArrayList<>())
                            .build();
                }).collect(Collectors.toList());
            }
        }

        return bomRows.stream().map(row -> {
            String childName;
            Long childItemId = row.getChildItemId();
            
            // Scale quantities using currentMultiplier
            BigDecimal scaledQuantity = row.getQuantity().multiply(currentMultiplier);

            if (row.getChildType() == BillOfMaterial.ChildType.product) {
                Product prod = productMap.get(row.getChildItemId());
                childName = prod != null ? prod.getProductName() : null;
            } else {
                childName = resolveRawMaterialName(row.getChildItemId(), rawMaterialMap);
            }
            
            // Recursively build children for products using scaledQuantity as multiplier
            List<EnhancedProductionPlanSummaryResponseDto.BomNodeDto> grandChildren = 
                row.getChildType() == BillOfMaterial.ChildType.product
                    ? buildBomTreeOptimized(row.getChildItemId(), scaledQuantity, bomMap, productMap, rawMaterialMap, productionCenterNamesMap, miniStoreMap)
                    : new ArrayList<>();
            
            BigDecimal unitCost = null;
            BigDecimal totalCost = null;
            java.time.LocalDate expireDate = null;
            BigDecimal currentStock = null;
            
            if ("raw_material".equals(row.getChildType().name())) {
                RawMaterial rawMaterial = rawMaterialMap.get(row.getChildItemId());
                if (rawMaterial == null) {
                    rawMaterial = rawMaterialRepository.findById(row.getChildItemId()).orElse(null);
                }
                if (rawMaterial != null) {
                    unitCost = BigDecimal.valueOf(rawMaterial.getUnitCost());
                    totalCost = scaledQuantity.multiply(unitCost);
                    expireDate = rawMaterial.getExpireDate();
                    currentStock = BigDecimal.valueOf(getAggregatedCurrentStock(rawMaterial));
                }
            }
            
            // Build mini store availability using cached data
            java.util.List<MiniStoreAvailabilityDto> miniStoreAvailability = miniStoreRepository.findAll().stream().map(store -> {
                List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = miniStoreMap.getOrDefault(row.getChildItemId(), new ArrayList<>());
                java.math.BigDecimal qty = items.stream()
                        .filter(i -> ("product".equals(row.getChildType().name()) && i.getProductId() != null && i.getProductId().equals(row.getChildItemId()))
                                || ("raw_material".equals(row.getChildType().name()) && i.getRawMaterialId() != null && i.getRawMaterialId().equals(row.getChildItemId())))
                        .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                        .filter(java.util.Objects::nonNull)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                return MiniStoreAvailabilityDto.builder()
                        .miniStoreId(store.getMiniStoreId())
                        .miniStoreName(store.getName())
                        .availableQty(qty)
                        .build();
            }).collect(Collectors.toList());
            
            return EnhancedProductionPlanSummaryResponseDto.BomNodeDto.builder()
                    .childItemId(childItemId)
                    .childName(childName)
                    .childType(row.getChildType().name())
                    .quantity(scaledQuantity)
                    .unit(row.getUnit())
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .productionCenter(productionCenterNamesMap.getOrDefault(row.getProductionCenterId(), "Unknown"))
                    .miniStoreAvailability(miniStoreAvailability)
                    .expireDate(expireDate)
                    .currentStock(currentStock)
                    .children(grandChildren)
                    .build();
        }).collect(Collectors.toList());
    }
    
    // ========== NEW PRODUCTION PLAN SUMMARY METHODS ==========
    
    /**
     * Builds an individual production plan with its own summary and consolidated products
     */
    private ProductionPlanSummaryResponseDto.ProductionPlanDto buildIndividualProductionPlan(
            ProductionPlan plan,
            Map<Long, Product> productMap,
            Map<Long, RawMaterial> rawMaterialMap,
            Map<String, List<RawMaterial>> rawMaterialBatchMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores,
            Map<Long, String> packUomMap) {
        
        // Get production plan items for this specific plan
        List<ProductionPlanItem> planItems = productionPlanItemRepository.findByProductionPlan_Id(plan.getId());
        
        // Build summary products for this plan
        List<ProductionPlanSummaryResponseDto.SummaryProductDto> summaryProducts = buildSummaryProducts(
                planItems, productMap, productionCenterNamesMap, bomMap);
        
        // Build consolidated products (semi-products) for this plan
        List<ProductionPlanSummaryResponseDto.ConsolidatedProductDto> consolidatedProducts = buildConsolidatedProducts(
                planItems, productMap, rawMaterialMap, rawMaterialBatchMap, productionCenterNamesMap, bomMap, miniStoreMap, miniStores, packUomMap);
        
        // Build raw materials for this plan (only direct raw-material items, not BOM-derived)
        List<ProductionPlanSummaryResponseDto.RawMaterialSummaryDto> rawMaterials = buildRawMaterialsForPlan(
                planItems, rawMaterialMap, rawMaterialBatchMap, productionCenterNamesMap, miniStoreMap, miniStores, packUomMap);
        
        return ProductionPlanSummaryResponseDto.ProductionPlanDto.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .productionDate(plan.getPlanDate() != null ? plan.getPlanDate().toString() : null)
                .createdBy("Manager") // You can get this from user context
                .remarks(plan.getNotes())
                .totalEstimatedCost(plan.getTotalEstimatedCost())
                .status(plan.getStatus() != null ? plan.getStatus().name() : null)
                .summaryProducts(summaryProducts)
                .consolidatedProducts(consolidatedProducts)
                .rawMaterials(rawMaterials)
                .build();
    }
    
    /**
     * Builds summary products for the new response format
     */
    private String resolveProductUnit(Product product) {
        if (product != null && product.getUnitOfMeasure() != null && !product.getUnitOfMeasure().trim().isEmpty()) {
            return product.getUnitOfMeasure();
        }
        return "pcs";
    }

    private List<ProductionPlanSummaryResponseDto.SummaryProductDto> buildSummaryProducts(
            List<ProductionPlanItem> planItems,
            Map<Long, Product> productMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<BillOfMaterial>> bomMap) {
        
        return planItems.stream()
                .filter(item -> item.getProductId() != null) // Only process products, not raw materials
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    String productionCenter = "Unknown";
                    if (product != null && product.getProductionCenterId() != null) {
                        productionCenter = productionCenterNamesMap.getOrDefault(product.getProductionCenterId(), "Unknown");
                    }
                    
                    // Get required semi-products from BOM
                    List<ProductionPlanSummaryResponseDto.RequiredSemiProductDto> requiredSemiProducts = 
                        buildRequiredSemiProducts(item.getProductId(), item.getQuantity(), bomMap, productMap);
                    
                    return ProductionPlanSummaryResponseDto.SummaryProductDto.builder()
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .plannedQuantity(item.getQuantity())
                            .unit(resolveProductUnit(product))
                            .productionCenter(productionCenter)
                            .category(product != null ? product.getCategory() : null)
                            .requiredSemiProducts(requiredSemiProducts)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Builds required semi-products for a product
     */
    private List<ProductionPlanSummaryResponseDto.RequiredSemiProductDto> buildRequiredSemiProducts(
            Long productId,
            Integer plannedQuantity,
            Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, Product> productMap) {
        
        List<BillOfMaterial> bomRows = bomMap.getOrDefault(productId, new ArrayList<>());
        
        return bomRows.stream()
                .filter(bom -> bom.getChildType() == BillOfMaterial.ChildType.product) // Only semi-products
                .map(bom -> {
                    Product semiProduct = productMap.get(bom.getChildItemId());
                    String semiProductName = semiProduct != null ? semiProduct.getProductName() : "Unknown";
                    
                    return ProductionPlanSummaryResponseDto.RequiredSemiProductDto.builder()
                            .semiProductRefId(bom.getChildItemId())
                            .semiProductName(semiProductName)
                            .quantityUsed(bom.getQuantity().multiply(BigDecimal.valueOf(plannedQuantity)))
                            .unit(bom.getUnit())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Builds consolidated products (semi-products) with deduplication
     */
    private List<ProductionPlanSummaryResponseDto.ConsolidatedProductDto> buildConsolidatedProducts(
            List<ProductionPlanItem> planItems,
            Map<Long, Product> productMap,
            Map<Long, RawMaterial> rawMaterialMap,
            Map<String, List<RawMaterial>> rawMaterialBatchMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores,
            Map<Long, String> packUomMap) {
        
        // Collect all semi-products used across all products
        Map<Long, SemiProductUsage> semiProductUsageMap = new HashMap<>();
        
        for (ProductionPlanItem item : planItems) {
            if (item.getProductId() != null) {
                double netQty = item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0;
                if (item.getReservedFromMiniStore() != null && item.getReservedFromMiniStore() > 0) {
                    netQty = Math.max(0.0, netQty - item.getReservedFromMiniStore().doubleValue());
                }
                if (netQty <= 0.00001) {
                    continue;
                }

                // Check if this item's child products are already present in planItems
                List<BillOfMaterial> bomRows = bomMap.getOrDefault(item.getProductId(), new ArrayList<>());
                boolean hasChildProductsInPlan = false;
                for (BillOfMaterial bom : bomRows) {
                    if (bom.getChildType() == BillOfMaterial.ChildType.product) {
                        Long childId = bom.getChildItemId();
                        if (planItems.stream().anyMatch(pi -> childId.equals(pi.getProductId()))) {
                            hasChildProductsInPlan = true;
                            break;
                        }
                    }
                }

                if (!hasChildProductsInPlan) {
                    collectSemiProductUsage(item.getProductId(), BigDecimal.valueOf(netQty), item.getProductName(), 
                        bomMap, productMap, semiProductUsageMap);
                }
            }
        }
        
        // Build consolidated products
        return semiProductUsageMap.values().stream()
                .map(usage -> {
                    Product semiProduct = productMap.get(usage.semiProductId);
                    String productionCenter = "Unknown";
                    if (semiProduct != null && semiProduct.getProductionCenterId() != null) {
                        productionCenter = productionCenterNamesMap.getOrDefault(semiProduct.getProductionCenterId(), "Unknown");
                    }
                    
                    // Build usage breakdown with nested semi-products
                    List<ProductionPlanSummaryResponseDto.UsageBreakdownDto> usageBreakdown = 
                        buildUsageBreakdownWithNestedSemiProducts(usage, planItems, productMap, rawMaterialMap, rawMaterialBatchMap, 
                            productionCenterNamesMap, bomMap, miniStoreMap, miniStores, packUomMap);
                    
                    // Calculate total quantity - usage.totalQuantity includes direct usage from plan items
                    // If a nested semi-product is in usageBreakdown, its quantity is already included in usage.totalQuantity
                    // But we need to also check for intermediate products (not in plan items) that use the parent
                    BigDecimal totalQtyWithNested = usage.totalQuantity;
                    
                    // Check for intermediate products that use this parent but aren't in direct usage
                    // This happens when a product uses the parent, but that product itself is not in plan items
                    // (e.g., "Bread Roll" uses "Bread Roll Dough", but "Bread Roll" is not in plan items)
                    for (Map.Entry<Long, Product> entry : productMap.entrySet()) {
                        Long productId = entry.getKey();
                        if (productId.equals(usage.semiProductId)) continue; // Skip self
                        
                        List<BillOfMaterial> productBom = bomMap.getOrDefault(productId, new ArrayList<>());
                        boolean usesParent = productBom.stream()
                            .anyMatch(bom -> bom.getChildType() == BillOfMaterial.ChildType.product &&
                                           bom.getChildItemId().equals(usage.semiProductId));
                        
                        if (usesParent) {
                            // This product uses the parent, check if it's already in usageBreakdown
                            Product product = entry.getValue();
                            String productName = product != null ? product.getProductName() : "Unknown";
                            boolean alreadyInUsage = usageBreakdown.stream()
                                .anyMatch(ub -> ub.getUsedFor().equals(productName));
                            
                            if (!alreadyInUsage) {
                                // This is an intermediate product, calculate its quantity from its usage
                                IntermediateProductQty qtyInfo = calculateIntermediateProductQuantity(
                                    productId, usage.semiProductId, planItems, bomMap);
                                
                                if (qtyInfo.parentQtyNeeded.compareTo(BigDecimal.ZERO) > 0) {
                                    totalQtyWithNested = totalQtyWithNested.add(qtyInfo.parentQtyNeeded);
                                    
                                    // Check if this intermediate product is a semi-product and add to usageBreakdown
                                    boolean isSemiProduct = productBom.stream()
                                        .anyMatch(row -> row.getChildType() == BillOfMaterial.ChildType.product);
                                    
                                    if (isSemiProduct) {
                                        // Use intermediateQty (total quantity of intermediate product needed) for nested structure
                                        ProductionPlanSummaryResponseDto.ConsolidatedProductDto nestedSemiProduct = 
                                            buildNestedSemiProduct(productId, qtyInfo.intermediateQty, planItems,
                                                productMap, rawMaterialMap, rawMaterialBatchMap, productionCenterNamesMap, bomMap, miniStoreMap, miniStores, packUomMap);
                                        
                                        // Use parentQtyNeeded (quantity of parent needed for this intermediate product) in usageBreakdown
                                        ProductionPlanSummaryResponseDto.UsageBreakdownDto intermediateUsage = 
                                            ProductionPlanSummaryResponseDto.UsageBreakdownDto.builder()
                                                .usedFor(productName)
                                                .quantityUsed(qtyInfo.parentQtyNeeded)
                                                .unit(resolveProductUnit(productMap.get(findProductIdByName(productName, planItems, productMap))))
                                                .semiProduct(nestedSemiProduct)
                                                .build();
                                        
                                        usageBreakdown.add(intermediateUsage);
                                    }
                                }
                            }
                        }
                    }
                    
                    // Build children (raw materials and other semi-products) with hierarchical structure
                    List<ProductionPlanSummaryResponseDto.ChildItemDto> children = buildConsolidatedProductChildren(
                            usage.semiProductId, totalQtyWithNested, bomMap, productMap, rawMaterialMap, rawMaterialBatchMap,
                            productionCenterNamesMap, miniStoreMap, miniStores, planItems, packUomMap);
                    
                    return ProductionPlanSummaryResponseDto.ConsolidatedProductDto.builder()
                            .semiProductId(usage.semiProductId)
                            .semiProductName(usage.semiProductName)
                            .productionCenter(productionCenter)
                            .totalRequiredQty(totalQtyWithNested)
                            .category(semiProduct != null ? semiProduct.getCategory() : null)
                            .unit(resolveProductUnit(semiProduct))
                            .usageBreakdown(usageBreakdown)
                            .children(children)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Collects semi-product usage information
     */
    private void collectSemiProductUsage(Long productId, BigDecimal plannedQuantity, String productName,
            Map<Long, List<BillOfMaterial>> bomMap, Map<Long, Product> productMap,
            Map<Long, SemiProductUsage> semiProductUsageMap) {
        
        List<BillOfMaterial> bomRows = bomMap.getOrDefault(productId, new ArrayList<>());
        boolean hasDirectRawMaterials = false;
        
        for (BillOfMaterial bom : bomRows) {
            if (bom.getChildType() == BillOfMaterial.ChildType.product) {
                Long semiProductId = bom.getChildItemId();
                Product semiProduct = productMap.get(semiProductId);
                String semiProductName = semiProduct != null ? semiProduct.getProductName() : "Unknown";
                BigDecimal quantityUsed = bom.getQuantity().multiply(plannedQuantity);
                
                SemiProductUsage usage = semiProductUsageMap.computeIfAbsent(semiProductId, 
                    id -> new SemiProductUsage(id, semiProductName, BigDecimal.ZERO, new ArrayList<>()));
                
                usage.totalQuantity = usage.totalQuantity.add(quantityUsed);
                usage.usageDetails.add(new UsageDetail(productName, quantityUsed, bom.getUnit()));
            } else if (bom.getChildType() == BillOfMaterial.ChildType.raw_material) {
                hasDirectRawMaterials = true;
            }
        }

        if (hasDirectRawMaterials) {
            Product product = productMap.get(productId);
            String pName = product != null ? product.getProductName() : productName;
            BigDecimal qty = plannedQuantity;

            SemiProductUsage selfUsage = semiProductUsageMap.computeIfAbsent(productId,
                id -> new SemiProductUsage(id, pName, BigDecimal.ZERO, new ArrayList<>()));
            
            selfUsage.totalQuantity = selfUsage.totalQuantity.add(qty);
            selfUsage.usageDetails.add(new UsageDetail(pName, qty, resolveProductUnit(product)));
        }
    }
    
    /**
     * Builds usage breakdown with nested semi-products
     * If a product that uses the parent is itself a semi-product (has child products),
     * include its nested structure in the usageBreakdown
     */
    private List<ProductionPlanSummaryResponseDto.UsageBreakdownDto> buildUsageBreakdownWithNestedSemiProducts(
            SemiProductUsage parentUsage, List<ProductionPlanItem> planItems,
            Map<Long, Product> productMap, Map<Long, RawMaterial> rawMaterialMap,
            Map<String, List<RawMaterial>> rawMaterialBatchMap,
            Map<Long, String> productionCenterNamesMap, Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores,
            Map<Long, String> packUomMap) {
        
        List<ProductionPlanSummaryResponseDto.UsageBreakdownDto> usageBreakdown = new ArrayList<>();
        
        // Group usage details by product name to aggregate quantities
        Map<String, UsageAggregate> usageMap = new HashMap<>();
        
        for (UsageDetail detail : parentUsage.usageDetails) {
            UsageAggregate agg = usageMap.computeIfAbsent(detail.usedFor,
                k -> new UsageAggregate(detail.usedFor, BigDecimal.ZERO, detail.unit));
            agg.totalQuantity = agg.totalQuantity.add(detail.quantityUsed);
        }
        
        // Build usage breakdown with nested semi-products
        for (UsageAggregate agg : usageMap.values()) {
            // Find the product by name to check if it's a semi-product
            Long productId = findProductIdByName(agg.productName, planItems, productMap);
            boolean isSemiProduct = false;
            ProductionPlanSummaryResponseDto.ConsolidatedProductDto nestedSemiProduct = null;
            
            if (productId != null) {
                // Check if this product is itself a semi-product (has child products)
                List<BillOfMaterial> productBomRows = bomMap.getOrDefault(productId, new ArrayList<>());
                isSemiProduct = productBomRows.stream()
                    .anyMatch(row -> row.getChildType() == BillOfMaterial.ChildType.product);
                
                if (isSemiProduct) {
                    // Build nested semi-product structure
                    nestedSemiProduct = buildNestedSemiProduct(productId, agg.totalQuantity, planItems,
                        productMap, rawMaterialMap, rawMaterialBatchMap, productionCenterNamesMap, bomMap, miniStoreMap, miniStores, packUomMap);
                }
            }
            
            ProductionPlanSummaryResponseDto.UsageBreakdownDto usageDto = 
                ProductionPlanSummaryResponseDto.UsageBreakdownDto.builder()
                    .usedFor(agg.productName)
                    .quantityUsed(agg.totalQuantity)
                    .unit(agg.unit)
                    .semiProduct(nestedSemiProduct)
                    .build();
            
            usageBreakdown.add(usageDto);
        }
        
        return usageBreakdown;
    }
    
    /**
     * Calculates the total quantity needed for an intermediate product
     * and returns both the intermediate product quantity and the parent quantity needed
     */
    private IntermediateProductQty calculateIntermediateProductQuantity(Long intermediateProductId, Long parentSemiProductId,
            List<ProductionPlanItem> planItems, Map<Long, List<BillOfMaterial>> bomMap) {
        
        BigDecimal intermediateQty = BigDecimal.ZERO;
        
        // Find all plan items that use the intermediate product, and calculate how much intermediate product is needed
        for (ProductionPlanItem planItem : planItems) {
            if (planItem.getProductId() == null) continue;
            
            List<BillOfMaterial> bomRows = bomMap.getOrDefault(planItem.getProductId(), new ArrayList<>());
            for (BillOfMaterial bom : bomRows) {
                if (bom.getChildType() == BillOfMaterial.ChildType.product &&
                    bom.getChildItemId().equals(intermediateProductId)) {
                    // This plan item uses the intermediate product
                    BigDecimal qtyNeeded = bom.getQuantity().multiply(BigDecimal.valueOf(planItem.getQuantity()));
                    intermediateQty = intermediateQty.add(qtyNeeded);
                }
            }
        }
        
        // Now calculate how much of the parent semi-product is needed for this intermediate product
        List<BillOfMaterial> intermediateBom = bomMap.getOrDefault(intermediateProductId, new ArrayList<>());
        BigDecimal parentQtyNeeded = BigDecimal.ZERO;
        
        for (BillOfMaterial bom : intermediateBom) {
            if (bom.getChildType() == BillOfMaterial.ChildType.product &&
                bom.getChildItemId().equals(parentSemiProductId)) {
                // Intermediate product uses the parent, calculate quantity
                parentQtyNeeded = bom.getQuantity().multiply(intermediateQty);
                break; // Assuming one BOM row for parent
            }
        }
        
        return new IntermediateProductQty(intermediateQty, parentQtyNeeded);
    }
    
    /**
     * Finds product ID by product name from plan items
     */
    private Long findProductIdByName(String productName, List<ProductionPlanItem> planItems,
            Map<Long, Product> productMap) {
        
        for (ProductionPlanItem item : planItems) {
            if (item.getProductName() != null && item.getProductName().equals(productName)) {
                return item.getProductId();
            }
        }
        
        // Also check in product map if not found in plan items
        for (Map.Entry<Long, Product> entry : productMap.entrySet()) {
            if (entry.getValue() != null && productName.equals(entry.getValue().getProductName())) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Builds a nested semi-product structure for a product that uses the parent
     */
    private ProductionPlanSummaryResponseDto.ConsolidatedProductDto buildNestedSemiProduct(
            Long nestedSemiProductId, BigDecimal totalQuantity, List<ProductionPlanItem> planItems,
            Map<Long, Product> productMap, Map<Long, RawMaterial> rawMaterialMap,
            Map<String, List<RawMaterial>> rawMaterialBatchMap,
            Map<Long, String> productionCenterNamesMap, Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores,
            Map<Long, String> packUomMap) {
        
        Product nestedSemiProduct = productMap.get(nestedSemiProductId);
        String productionCenter = "Unknown";
        if (nestedSemiProduct != null && nestedSemiProduct.getProductionCenterId() != null) {
            productionCenter = productionCenterNamesMap.getOrDefault(nestedSemiProduct.getProductionCenterId(), "Unknown");
        }
        
        // Find products that use this nested semi-product
        Map<String, UsageAggregate> nestedUsageMap = new HashMap<>();
        
        for (ProductionPlanItem planItem : planItems) {
            if (planItem.getProductId() == null) continue;
            
            // Check if this product uses the nested semi-product
            List<BillOfMaterial> bomRows = bomMap.getOrDefault(planItem.getProductId(), new ArrayList<>());
            for (BillOfMaterial bom : bomRows) {
                if (bom.getChildType() == BillOfMaterial.ChildType.product && 
                    bom.getChildItemId().equals(nestedSemiProductId)) {
                    BigDecimal quantityUsed = bom.getQuantity().multiply(BigDecimal.valueOf(planItem.getQuantity()));
                    UsageAggregate agg = nestedUsageMap.computeIfAbsent(planItem.getProductName(),
                        k -> new UsageAggregate(planItem.getProductName(), BigDecimal.ZERO, bom.getUnit()));
                    agg.totalQuantity = agg.totalQuantity.add(quantityUsed);
                }
            }
        }
        
        // Build usage breakdown for nested semi-product
        List<ProductionPlanSummaryResponseDto.UsageBreakdownDto> nestedUsageBreakdown = nestedUsageMap.values().stream()
            .map(agg -> ProductionPlanSummaryResponseDto.UsageBreakdownDto.builder()
                .usedFor(agg.productName)
                .quantityUsed(agg.totalQuantity)
                .unit(agg.unit)
                .semiProduct(null) // Don't nest further (max 2 levels based on requirement)
                .build())
            .collect(Collectors.toList());
        
        // Build children (raw materials only, no nested products in children)
        List<ProductionPlanSummaryResponseDto.ChildItemDto> nestedChildren = 
            buildNestedSemiProductChildren(nestedSemiProductId, totalQuantity, bomMap, productMap, 
                rawMaterialMap, rawMaterialBatchMap, productionCenterNamesMap, miniStoreMap, miniStores, packUomMap);
        
        return ProductionPlanSummaryResponseDto.ConsolidatedProductDto.builder()
                .semiProductId(nestedSemiProductId)
                .semiProductName(nestedSemiProduct != null ? nestedSemiProduct.getProductName() : "Unknown")
                .productionCenter(productionCenter)
                .totalRequiredQty(totalQuantity)
                .category(nestedSemiProduct != null ? nestedSemiProduct.getCategory() : null)
                .unit(resolveProductUnit(nestedSemiProduct))
                .usageBreakdown(nestedUsageBreakdown)
                .children(nestedChildren)
                .build();
    }
    
    /**
     * Builds children for nested semi-product (raw materials only, no nested products)
     */
    private List<ProductionPlanSummaryResponseDto.ChildItemDto> buildNestedSemiProductChildren(
            Long semiProductId, BigDecimal totalQuantity, Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, Product> productMap, Map<Long, RawMaterial> rawMaterialMap,
            Map<String, List<RawMaterial>> rawMaterialBatchMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores,
            Map<Long, String> packUomMap) {
        
        List<BillOfMaterial> bomRows = bomMap.getOrDefault(semiProductId, new ArrayList<>());
        
        // Only process raw materials (no nested products in children)
        Map<Long, ProductionPlanSummaryResponseDto.ChildItemDto> rawMaterialMap_dedup = new HashMap<>();
        
        for (BillOfMaterial bom : bomRows) {
            if (bom.getChildType() == BillOfMaterial.ChildType.raw_material) {
                Long rawMaterialId = bom.getChildItemId();
                BigDecimal scaledQuantity = bom.getQuantity().multiply(totalQuantity);
                
                ProductionPlanSummaryResponseDto.ChildItemDto existing = rawMaterialMap_dedup.get(rawMaterialId);
                
                if (existing != null) {
                    BigDecimal newQuantity = existing.getQuantity().add(scaledQuantity);
                    BigDecimal newTotalCost = newQuantity.multiply(existing.getUnitCost());
                    
                    ProductionPlanSummaryResponseDto.ChildItemDto updated = ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                            .childItemId(existing.getChildItemId())
                            .childRefId(existing.getChildRefId())
                            .childName(existing.getChildName())
                            .childType(existing.getChildType())
                            .quantity(newQuantity)
                            .unit(existing.getUnit())
                            .unitCost(existing.getUnitCost())
                            .totalCost(newTotalCost)
                            .productionCenter(existing.getProductionCenter())
                            .miniStoreAvailability(existing.getMiniStoreAvailability())
                            .expireDate(existing.getExpireDate())
                            .currentStock(existing.getCurrentStock())
                            .children(existing.getChildren())
                            .build();
                    
                    rawMaterialMap_dedup.put(rawMaterialId, updated);
                } else {
                    String childName = resolveRawMaterialName(rawMaterialId, rawMaterialMap);
                    RawMaterial rawMaterial = rawMaterialMap.get(rawMaterialId);
                    if (rawMaterial == null) {
                        rawMaterial = rawMaterialRepository.findById(rawMaterialId).orElse(null);
                    }
                    
                    BigDecimal unitCost = (rawMaterial != null && rawMaterial.getUnitCost() != null) ? BigDecimal.valueOf(rawMaterial.getUnitCost()) : BigDecimal.ZERO;
                    BigDecimal totalCost = scaledQuantity.multiply(unitCost);
                    
                    List<ProductionPlanSummaryResponseDto.MiniStoreAvailabilityDto> miniStoreAvailability = 
                        buildMiniStoreAvailability(rawMaterialId, "raw_material", miniStoreMap, miniStores);
                    
                    String displayUnit = resolveRawMaterialUnit(rawMaterialId, bom.getUnit(), rawMaterialMap, packUomMap);
                    
                    String itemCenterName = null;
                    if (bom.getProductionCenterId() != null) {
                        itemCenterName = productionCenterNamesMap.get(bom.getProductionCenterId());
                    }
                    if (itemCenterName == null || "Unknown".equalsIgnoreCase(itemCenterName)) {
                        Product parentProd = productMap.get(semiProductId);
                        if (parentProd != null && parentProd.getProductionCenterId() != null) {
                            itemCenterName = productionCenterNamesMap.get(parentProd.getProductionCenterId());
                        }
                    }
                    if (itemCenterName == null) {
                        itemCenterName = "Unknown";
                    }

                    ProductionPlanSummaryResponseDto.ChildItemDto rawMaterialChild = ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                            .childItemId(rawMaterialId)
                            .childRefId(rawMaterialId)
                            .childName(childName)
                            .childType("raw_material")
                            .quantity(scaledQuantity)
                            .unit(displayUnit)
                            .unitCost(unitCost)
                            .totalCost(totalCost)
                            .productionCenter(itemCenterName)
                            .miniStoreAvailability(miniStoreAvailability)
                            .expireDate(rawMaterial != null ? rawMaterial.getExpireDate() : null)
                            .currentStock(rawMaterial != null ? BigDecimal.valueOf(getAggregatedCurrentStock(rawMaterial)) : BigDecimal.ZERO)
                            .batches(buildBatchDetails(rawMaterial != null ? rawMaterial.getMaterialCode() : null, rawMaterialBatchMap, miniStoreMap, miniStores))
                            .children(new ArrayList<>())
                            .build();
                
                    rawMaterialMap_dedup.put(rawMaterialId, rawMaterialChild);
                }
            }
        }
        
        return new ArrayList<>(rawMaterialMap_dedup.values());
    }
    
    /**
     * Builds children for consolidated products with hierarchical structure:
     * - Products that directly use the parent semi-product
     *   - If product has no child products → show as final product
     *   - If product has child products → show as intermediate with nested final products that use it
     * - Raw materials show only at the parent level
     */
    private List<ProductionPlanSummaryResponseDto.ChildItemDto> buildConsolidatedProductChildren(
            Long semiProductId, BigDecimal totalQuantity, Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, Product> productMap, Map<Long, RawMaterial> rawMaterialMap,
            Map<String, List<RawMaterial>> rawMaterialBatchMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores,
            List<ProductionPlanItem> planItems,
            Map<Long, String> packUomMap) {
        
        // Find all products that directly use this parent semi-product (from BOM)
        List<BillOfMaterial> bomRows = bomMap.getOrDefault(semiProductId, new ArrayList<>());
        
        // Map to track products that directly use the parent and their quantities
        Map<Long, DirectProductUsage> directProductsMap = new HashMap<>();
        
        // Track raw materials separately (only at parent level)
        Map<Long, ProductionPlanSummaryResponseDto.ChildItemDto> rawMaterialMap_dedup = new HashMap<>();
        
        // First pass: collect products that directly use the parent
        for (BillOfMaterial bom : bomRows) {
            if (bom.getChildType() == BillOfMaterial.ChildType.product) {
                Long directProductId = bom.getChildItemId();
                Product directProduct = productMap.get(directProductId);
                String productName = directProduct != null ? directProduct.getProductName() : "Unknown";
                
                // Calculate total quantity needed from production plan items
                BigDecimal totalQty = calculateTotalQuantityForDirectProduct(semiProductId, directProductId, planItems, bomMap);
                
                // Check if this product has child products (intermediate) or not (final)
                List<BillOfMaterial> childBomRows = bomMap.getOrDefault(directProductId, new ArrayList<>());
                boolean hasChildProducts = childBomRows.stream()
                    .anyMatch(row -> row.getChildType() == BillOfMaterial.ChildType.product);
                
                directProductsMap.put(directProductId, new DirectProductUsage(
                    directProductId, productName, totalQty, hasChildProducts));
            } else {
                // Raw materials - deduplicate and accumulate
                Long rawMaterialId = bom.getChildItemId();
                BigDecimal scaledQuantity = bom.getQuantity().multiply(totalQuantity);
                
                ProductionPlanSummaryResponseDto.ChildItemDto existing = rawMaterialMap_dedup.get(rawMaterialId);
                
                if (existing != null) {
                    BigDecimal newQuantity = existing.getQuantity().add(scaledQuantity);
                    BigDecimal newTotalCost = newQuantity.multiply(existing.getUnitCost());
                    
                    ProductionPlanSummaryResponseDto.ChildItemDto updated = ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                            .childItemId(existing.getChildItemId())
                            .childRefId(existing.getChildRefId())
                            .childName(existing.getChildName())
                            .childType(existing.getChildType())
                            .quantity(newQuantity)
                            .unit(existing.getUnit())
                            .unitCost(existing.getUnitCost())
                            .totalCost(newTotalCost)
                            .productionCenter(existing.getProductionCenter())
                            .miniStoreAvailability(existing.getMiniStoreAvailability())
                            .expireDate(existing.getExpireDate())
                            .currentStock(existing.getCurrentStock())
                            .children(existing.getChildren())
                            .build();
                    
                    rawMaterialMap_dedup.put(rawMaterialId, updated);
                } else {
                    String childName = resolveRawMaterialName(rawMaterialId, rawMaterialMap);
                    RawMaterial rawMaterial = rawMaterialMap.get(rawMaterialId);
                    if (rawMaterial == null) {
                        rawMaterial = rawMaterialRepository.findById(rawMaterialId).orElse(null);
                    }
                    
                    BigDecimal unitCost = (rawMaterial != null && rawMaterial.getUnitCost() != null) ? BigDecimal.valueOf(rawMaterial.getUnitCost()) : BigDecimal.ZERO;
                    BigDecimal totalCost = scaledQuantity.multiply(unitCost);
                    
                    List<ProductionPlanSummaryResponseDto.MiniStoreAvailabilityDto> miniStoreAvailability = 
                        buildMiniStoreAvailability(rawMaterialId, "raw_material", miniStoreMap, miniStores);
                    
                    String displayUnit = resolveRawMaterialUnit(rawMaterialId, bom.getUnit(), rawMaterialMap, packUomMap);
                    
                    String itemCenterName = null;
                    if (bom.getProductionCenterId() != null) {
                        itemCenterName = productionCenterNamesMap.get(bom.getProductionCenterId());
                    }
                    if (itemCenterName == null || "Unknown".equalsIgnoreCase(itemCenterName)) {
                        Product parentProd = productMap.get(semiProductId);
                        if (parentProd != null && parentProd.getProductionCenterId() != null) {
                            itemCenterName = productionCenterNamesMap.get(parentProd.getProductionCenterId());
                        }
                    }
                    if (itemCenterName == null) {
                        itemCenterName = "Unknown";
                    }

                    ProductionPlanSummaryResponseDto.ChildItemDto rawMaterialChild = ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                            .childItemId(rawMaterialId)
                            .childRefId(rawMaterialId)
                            .childName(childName)
                            .childType("raw_material")
                            .quantity(scaledQuantity)
                            .unit(displayUnit)
                            .unitCost(unitCost)
                            .totalCost(totalCost)
                            .productionCenter(itemCenterName)
                            .miniStoreAvailability(miniStoreAvailability)
                            .expireDate(rawMaterial != null ? rawMaterial.getExpireDate() : null)
                            .currentStock(rawMaterial != null ? BigDecimal.valueOf(getAggregatedCurrentStock(rawMaterial)) : BigDecimal.ZERO)
                            .batches(buildBatchDetails(rawMaterial != null ? rawMaterial.getMaterialCode() : null, rawMaterialBatchMap, miniStoreMap, miniStores))
                            .children(new ArrayList<>())
                            .build();
                    
                    rawMaterialMap_dedup.put(rawMaterialId, rawMaterialChild);
                }
            }
        }
        
        // Build child items with hierarchical structure
        List<ProductionPlanSummaryResponseDto.ChildItemDto> childItems = new ArrayList<>();
        
        for (DirectProductUsage directUsage : directProductsMap.values()) {
            Product product = productMap.get(directUsage.productId);
            
            if (directUsage.hasChildProducts) {
                // Intermediate product: find final products that use this intermediate product
                List<ProductionPlanSummaryResponseDto.ChildItemDto> finalProductChildren = 
                    buildFinalProductChildrenForIntermediate(directUsage.productId, planItems, productMap, 
                        productionCenterNamesMap, bomMap, miniStoreMap, miniStores);
                
                ProductionPlanSummaryResponseDto.ChildItemDto intermediateProduct = ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                        .childItemId(directUsage.productId)
                        .childRefId(directUsage.productId)
                        .childName(directUsage.productName)
                        .childType("product")
                        .quantity(directUsage.totalQuantity)
                        .unit(resolveProductUnit(product))
                        .unitCost(null)
                        .totalCost(null)
                        .productionCenter(productionCenterNamesMap.getOrDefault(
                            product != null && product.getProductionCenterId() != null 
                                ? product.getProductionCenterId() : null, "Unknown"))
                        .miniStoreAvailability(buildMiniStoreAvailability(directUsage.productId, "product", miniStoreMap, miniStores))
                        .expireDate(null)
                        .currentStock(null)
                        .batches(new ArrayList<>())
                        .children(finalProductChildren)
                        .build();
                
                childItems.add(intermediateProduct);
            } else {
                // Final product: show directly
                ProductionPlanSummaryResponseDto.ChildItemDto finalProduct = ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                        .childItemId(directUsage.productId)
                        .childRefId(directUsage.productId)
                        .childName(directUsage.productName)
                        .childType("product")
                        .quantity(directUsage.totalQuantity)
                        .unit(resolveProductUnit(product))
                        .unitCost(null)
                        .totalCost(null)
                        .productionCenter(productionCenterNamesMap.getOrDefault(
                            product != null && product.getProductionCenterId() != null 
                                ? product.getProductionCenterId() : null, "Unknown"))
                        .miniStoreAvailability(buildMiniStoreAvailability(directUsage.productId, "product", miniStoreMap, miniStores))
                        .expireDate(null)
                        .currentStock(null)
                        .batches(new ArrayList<>())
                        .children(new ArrayList<>())
                        .build();
                
                childItems.add(finalProduct);
            }
        }
        
        // Add raw materials at the end (parent level only)
        childItems.addAll(rawMaterialMap_dedup.values());
        
        return childItems;
    }
    
    /**
     * Calculates total quantity needed for a direct product that uses the parent semi-product.
     * This sums up quantities from:
     * 1. Final products that use the direct product
     * 2. The direct product itself if it's in the plan
     */
    private BigDecimal calculateTotalQuantityForDirectProduct(Long parentSemiProductId, Long directProductId,
            List<ProductionPlanItem> planItems, Map<Long, List<BillOfMaterial>> bomMap) {
        
        BigDecimal totalQty = BigDecimal.ZERO;
        
        // Method 1: Find final products that use the direct product
        for (ProductionPlanItem planItem : planItems) {
            if (planItem.getProductId() == null) continue;
            
            // Check if this product uses the direct product
            List<BillOfMaterial> bomRows = bomMap.getOrDefault(planItem.getProductId(), new ArrayList<>());
            for (BillOfMaterial bom : bomRows) {
                if (bom.getChildType() == BillOfMaterial.ChildType.product && 
                    bom.getChildItemId().equals(directProductId)) {
                    // Calculate quantity: plan quantity * BOM ratio
                    BigDecimal qty = bom.getQuantity().multiply(BigDecimal.valueOf(planItem.getQuantity()));
                    totalQty = totalQty.add(qty);
                }
            }
        }
        
        // Method 2: If the direct product itself is in the plan, get its quantity from the parent's BOM
        for (ProductionPlanItem planItem : planItems) {
            if (planItem.getProductId() != null && planItem.getProductId().equals(directProductId)) {
                // Check if this product uses the parent
                List<BillOfMaterial> bomRows = bomMap.getOrDefault(directProductId, new ArrayList<>());
                for (BillOfMaterial bom : bomRows) {
                    if (bom.getChildType() == BillOfMaterial.ChildType.product && 
                        bom.getChildItemId().equals(parentSemiProductId)) {
                        // This direct product uses the parent, so add its quantity
                        BigDecimal qty = bom.getQuantity().multiply(BigDecimal.valueOf(planItem.getQuantity()));
                        totalQty = totalQty.add(qty);
                    }
                }
            }
        }
        
        return totalQty;
    }
    
    /**
     * Builds final product children for intermediate products
     * Finds which final products use the intermediate product
     */
    private List<ProductionPlanSummaryResponseDto.ChildItemDto> buildFinalProductChildrenForIntermediate(
            Long intermediateProductId, List<ProductionPlanItem> planItems, Map<Long, Product> productMap,
            Map<Long, String> productionCenterNamesMap, Map<Long, List<BillOfMaterial>> bomMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores) {
        
        // Find final products that use this intermediate product
        Map<Long, FinalProductInfo> finalProductMap = new HashMap<>();
        
        for (ProductionPlanItem planItem : planItems) {
            if (planItem.getProductId() == null) continue;
            
            // Check if this product uses the intermediate product
            List<BillOfMaterial> bomRows = bomMap.getOrDefault(planItem.getProductId(), new ArrayList<>());
            for (BillOfMaterial bom : bomRows) {
                if (bom.getChildType() == BillOfMaterial.ChildType.product && 
                    bom.getChildItemId().equals(intermediateProductId)) {
                    // This final product uses the intermediate product
                    BigDecimal quantityUsed = bom.getQuantity().multiply(BigDecimal.valueOf(planItem.getQuantity()));
                    FinalProductInfo info = finalProductMap.computeIfAbsent(planItem.getProductId(),
                        id -> new FinalProductInfo(id, planItem.getProductName(), BigDecimal.ZERO));
                    info.totalQuantity = info.totalQuantity.add(quantityUsed);
                }
            }
        }
        
        // Build child items for final products
        return finalProductMap.values().stream()
            .map(info -> {
                Product product = productMap.get(info.productId);
                return ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                        .childItemId(info.productId)
                        .childRefId(info.productId)
                        .childName(info.productName)
                        .childType("product")
                        .quantity(info.totalQuantity)
                        .unit(resolveProductUnit(product))
                        .unitCost(null)
                        .totalCost(null)
                        .productionCenter(productionCenterNamesMap.getOrDefault(
                            product != null && product.getProductionCenterId() != null 
                                ? product.getProductionCenterId() : null, "Unknown"))
                        .miniStoreAvailability(buildMiniStoreAvailability(info.productId, "product", miniStoreMap, miniStores))
                        .expireDate(null)
                        .currentStock(null)
                        .batches(new ArrayList<>())
                        .children(new ArrayList<>())
                        .build();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Builds reverse mapping of which products use a given semi-product
     */
    private Map<Long, List<UsageTracking>> buildProductUsageMap(
            Long semiProductId, List<ProductionPlanItem> planItems,
            Map<Long, List<BillOfMaterial>> bomMap, Map<Long, Product> productMap) {
        
        Map<Long, List<UsageTracking>> usageMap = new HashMap<>();
        
        for (ProductionPlanItem planItem : planItems) {
            if (planItem.getProductId() == null) continue;
            
            // Check if this product directly uses the semi-product
            List<BillOfMaterial> bomRows = bomMap.getOrDefault(planItem.getProductId(), new ArrayList<>());
            for (BillOfMaterial bom : bomRows) {
                if (bom.getChildType() == BillOfMaterial.ChildType.product && 
                    bom.getChildItemId().equals(semiProductId)) {
                    BigDecimal quantityUsed = bom.getQuantity().multiply(BigDecimal.valueOf(planItem.getQuantity()));
                    usageMap.computeIfAbsent(planItem.getProductId(), k -> new ArrayList<>())
                        .add(new UsageTracking(planItem.getProductId(), planItem.getProductName(), quantityUsed));
                }
            }
        }
        
        return usageMap;
    }
    
    
    /**
     * Merges two lists of children, deduplicating by childItemId and childType
     * For products: merges recursively and combines quantities
     * For raw materials: combines quantities and costs
     */
    private List<ProductionPlanSummaryResponseDto.ChildItemDto> mergeChildrenLists(
            List<ProductionPlanSummaryResponseDto.ChildItemDto> existingChildren,
            List<ProductionPlanSummaryResponseDto.ChildItemDto> newChildren) {
        
        if (existingChildren == null || existingChildren.isEmpty()) {
            return newChildren != null ? newChildren : new ArrayList<>();
        }
        if (newChildren == null || newChildren.isEmpty()) {
            return existingChildren;
        }
        
        Map<String, ProductionPlanSummaryResponseDto.ChildItemDto> mergedMap = new HashMap<>();
        
        // Add existing children
        for (ProductionPlanSummaryResponseDto.ChildItemDto child : existingChildren) {
            String key = child.getChildItemId() + ":" + child.getChildType();
            mergedMap.put(key, child);
        }
        
        // Merge with new children
        for (ProductionPlanSummaryResponseDto.ChildItemDto newChild : newChildren) {
            String key = newChild.getChildItemId() + ":" + newChild.getChildType();
            ProductionPlanSummaryResponseDto.ChildItemDto existing = mergedMap.get(key);
            
            if (existing != null) {
                // Merge existing with new child
                BigDecimal mergedQuantity = existing.getQuantity().add(newChild.getQuantity());
                
                if ("product".equals(newChild.getChildType())) {
                    // For products: merge recursively and combine quantities
                    List<ProductionPlanSummaryResponseDto.ChildItemDto> mergedGrandChildren = 
                        mergeChildrenLists(existing.getChildren(), newChild.getChildren());
                    
                    ProductionPlanSummaryResponseDto.ChildItemDto merged = ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                            .childItemId(existing.getChildItemId())
                            .childRefId(existing.getChildRefId())
                            .childName(existing.getChildName())
                            .childType(existing.getChildType())
                            .quantity(mergedQuantity)
                            .unit(existing.getUnit())
                            .unitCost(existing.getUnitCost())
                            .totalCost(existing.getTotalCost())
                            .productionCenter(existing.getProductionCenter())
                            .miniStoreAvailability(existing.getMiniStoreAvailability())
                            .expireDate(existing.getExpireDate())
                            .currentStock(existing.getCurrentStock())
                            .batches(existing.getBatches())
                            .children(mergedGrandChildren)
                            .build();
                } else if ("raw_material".equals(newChild.getChildType())) {
                    // For raw materials: combine quantities and costs
                    BigDecimal mergedTotalCost = mergedQuantity.multiply(existing.getUnitCost() != null ? existing.getUnitCost() : BigDecimal.ZERO);
                    
                    ProductionPlanSummaryResponseDto.ChildItemDto merged = ProductionPlanSummaryResponseDto.ChildItemDto.builder()
                            .childItemId(existing.getChildItemId())
                            .childRefId(existing.getChildRefId())
                            .childName(existing.getChildName())
                            .childType(existing.getChildType())
                            .quantity(mergedQuantity)
                            .unit(existing.getUnit())
                            .unitCost(existing.getUnitCost())
                            .totalCost(mergedTotalCost)
                            .productionCenter(existing.getProductionCenter())
                            .miniStoreAvailability(existing.getMiniStoreAvailability())
                            .expireDate(existing.getExpireDate())
                            .currentStock(existing.getCurrentStock())
                            .batches(existing.getBatches())
                            .children(existing.getChildren())
                            .build();
                    
                    mergedMap.put(key, merged);
                }
            } else {
                // New child, add it
                mergedMap.put(key, newChild);
            }
        }
        
        return new ArrayList<>(mergedMap.values());
    }
    
    /**
     * Builds mini store availability for an item
     */
    private List<ProductionPlanSummaryResponseDto.MiniStoreAvailabilityDto> buildMiniStoreAvailability(
            Long itemId, String itemType, Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores) {
        
        return miniStores.stream().map(store -> {
            // Get all mini store items for this store
            List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = miniStoreMap.getOrDefault(itemId, new ArrayList<>());
            
            BigDecimal qty = items.stream()
                    .filter(i -> i.getMiniStore() != null && i.getMiniStore().getMiniStoreId().equals(store.getMiniStoreId()))
                    .filter(i -> ("product".equals(itemType) && i.getProductId() != null && i.getProductId().equals(itemId))
                            || ("raw_material".equals(itemType) && i.getRawMaterialId() != null && i.getRawMaterialId().equals(itemId)))
                    .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return ProductionPlanSummaryResponseDto.MiniStoreAvailabilityDto.builder()
                    .miniStoreId(store.getMiniStoreId().longValue())
                    .miniStoreName(store.getName())
                    .availableQty(qty)
                    .build();
        }).collect(Collectors.toList());
    }
    
    /**
     * Builds batch details for a raw material
     */
    private List<ProductionPlanSummaryResponseDto.BatchDetailDto> buildBatchDetails(
            String materialCode, 
            Map<String, List<RawMaterial>> rawMaterialBatchMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores) {
        
        if (materialCode == null) return new ArrayList<>();
        
        List<RawMaterial> batches = rawMaterialBatchMap.getOrDefault(materialCode, new ArrayList<>());
        
        return batches.stream().map(batch -> {
            List<ProductionPlanSummaryResponseDto.MiniStoreAvailabilityDto> miniStoreAvailability = 
                buildMiniStoreAvailability(batch.getId(), "raw_material", miniStoreMap, miniStores);
                
            return ProductionPlanSummaryResponseDto.BatchDetailDto.builder()
                    .id(batch.getId())
                    .batchNo(batch.getBatchNo())
                    .currentStock(batch.getCurrentStock())
                    .expireDate(batch.getExpireDate())
                    .unitCost(batch.getUnitCost())
                    .miniStoreAvailability(miniStoreAvailability)
                    .build();
        }).collect(Collectors.toList());
    }
    
    /**
     * Resolves raw material name with fallback logic
     */
    private String resolveRawMaterialName(Long childItemId, Map<Long, RawMaterial> rawMaterialMap) {
        // First try direct lookup
        RawMaterial rawMaterial = rawMaterialMap.get(childItemId);
        if (rawMaterial != null) {
            return rawMaterial.getDisplayName();
        }
        
        // Fallback: try to find by any raw material with matching ID
        rawMaterial = rawMaterialRepository.findById(childItemId).orElse(null);
        if (rawMaterial != null) {
            return rawMaterial.getDisplayName();
        }
        
        log.warn("Raw material not found for childItemId: {}", childItemId);
        return "Unknown";
    }
    
    /**
     * Builds raw materials for a production plan
     * NOTE: Only includes direct raw-material items (where productId is null), NOT BOM-derived raw materials
     */
    private List<ProductionPlanSummaryResponseDto.RawMaterialSummaryDto> buildRawMaterialsForPlan(
            List<ProductionPlanItem> planItems,
            Map<Long, RawMaterial> rawMaterialMap,
            Map<String, List<RawMaterial>> rawMaterialBatchMap,
            Map<Long, String> productionCenterNamesMap,
            Map<Long, List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem>> miniStoreMap,
            java.util.List<com.plover.backerymanagmentsystem.manager.model.MiniStore> miniStores,
            Map<Long, String> packUomMap) {
        
        // Aggregate raw material quantities from direct raw-material plan items only
        // Use a map to deduplicate by rawMaterialId and sum quantities
        Map<Long, RmAgg> rmTotals = new HashMap<>();

        // Only process direct raw material items (productId is null, rawMaterialId is not null)
        for (ProductionPlanItem item : planItems) {
            if (item.getRawMaterialId() != null && item.getProductId() == null) {
                rmTotals.compute(item.getRawMaterialId(), (id, agg) -> {
                    java.math.BigDecimal add = java.math.BigDecimal.valueOf(item.getQuantity());
                    if (agg == null) return new RmAgg(add, item.getProductionCenterId());
                    agg.qty = agg.qty.add(add);
                    if (agg.productionCenterId == null) agg.productionCenterId = item.getProductionCenterId();
                    return agg;
                });
            }
        }

        // Build DTOs from deduplicated raw materials
        return rmTotals.entrySet().stream().map(entry -> {
            Long rawMaterialId = entry.getKey();
            RmAgg agg = entry.getValue();
            RawMaterial rawMaterial = rawMaterialMap.get(rawMaterialId);
            if (rawMaterial == null) {
                rawMaterial = rawMaterialRepository.findById(rawMaterialId).orElse(null);
            }
            if (rawMaterial == null) {
                log.warn("Raw material not found for ID: {} while building summary", rawMaterialId);
                return null;
            }

            String productionCenter = productionCenterNamesMap.getOrDefault(agg.productionCenterId, "Unknown");
            List<ProductionPlanSummaryResponseDto.MiniStoreAvailabilityDto> miniStoreAvailability = 
                buildMiniStoreAvailability(rawMaterialId, "raw_material", miniStoreMap, miniStores);

            List<ProductionPlanSummaryResponseDto.BatchDetailDto> batches = 
                buildBatchDetails(rawMaterial.getMaterialCode(), rawMaterialBatchMap, miniStoreMap, miniStores);

            int plannedQtyInt = agg.qty.intValue();
            double unitCost = rawMaterial.getUnitCost();
            double totalCost = unitCost * agg.qty.doubleValue();

            String displayUnit = resolveRawMaterialUnit(rawMaterialId, rawMaterial.getUnitOfMeasure(), rawMaterialMap, packUomMap);

            return ProductionPlanSummaryResponseDto.RawMaterialSummaryDto.builder()
                    .rawMaterialId(rawMaterial.getId())
                    .rawMaterialName(rawMaterial.getDisplayName())
                    .plannedQuantity(plannedQtyInt)
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .unitOfMeasure(displayUnit)
                    .productionCenter(productionCenter)
                    .expireDate(rawMaterial.getExpireDate())
                    .currentStock(getAggregatedCurrentStock(rawMaterial))
                    .miniStoreAvailability(miniStoreAvailability)
                    .batches(batches)
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    /**
     * Builds mini store availability for raw materials
     */
    private List<ProductionPlanSummaryResponseDto.MiniStoreAvailabilityDto> buildRawMaterialMiniStoreAvailability(
            Long rawMaterialId) {
        
        return miniStoreRepository.findAll().stream().map(store -> {
            List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> items = 
                miniStoreItemRepository.findByMiniStore_MiniStoreId(store.getMiniStoreId());
            
            BigDecimal qty = items.stream()
                    .filter(i -> i.getRawMaterialId() != null && i.getRawMaterialId().equals(rawMaterialId))
                    .map(com.plover.backerymanagmentsystem.manager.model.MiniStoreItem::getPhysicalQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            return ProductionPlanSummaryResponseDto.MiniStoreAvailabilityDto.builder()
                    .miniStoreId(store.getMiniStoreId().longValue())
                    .miniStoreName(store.getName())
                    .availableQty(qty)
                    .build();
        }).collect(Collectors.toList());
    }
    private Map<Long, String> loadAllPackUoms() {
        return packDetailsRepository.findAll().stream()
                .filter(pd -> pd.getRawMaterial() != null)
                .collect(Collectors.toMap(
                        pd -> pd.getRawMaterial().getId(),
                        pd -> pd.getPackUom(),
                        (existing, replacement) -> existing
                ));
    }

    private String resolveRawMaterialUnit(Long rawMaterialId, String originalUnit, Map<Long, RawMaterial> rawMaterialMap, Map<Long, String> packUomMap) {
        RawMaterial rm = rawMaterialMap.get(rawMaterialId);
        if (rm != null && "pack".equalsIgnoreCase(rm.getUnitOfMeasure())) {
            String packUom = packUomMap.get(rawMaterialId);
            if (packUom != null) {
                return packUom;
            }
        }
        return originalUnit;
    }

    // Helper classes for semi-product usage tracking
    private static class SemiProductUsage {
        Long semiProductId;
        String semiProductName;
        BigDecimal totalQuantity;
        List<UsageDetail> usageDetails;
        
        SemiProductUsage(Long semiProductId, String semiProductName, BigDecimal totalQuantity, List<UsageDetail> usageDetails) {
            this.semiProductId = semiProductId;
            this.semiProductName = semiProductName;
            this.totalQuantity = totalQuantity;
            this.usageDetails = usageDetails;
        }
    }
    
    private static class UsageDetail {
        String usedFor;
        BigDecimal quantityUsed;
        String unit;
        
        UsageDetail(String usedFor, BigDecimal quantityUsed, String unit) {
            this.usedFor = usedFor;
            this.quantityUsed = quantityUsed;
            this.unit = unit;
        }
    }

    // Helper aggregation class for raw materials
    private static class RmAgg {
        java.math.BigDecimal qty;
        Long productionCenterId;
        RmAgg(java.math.BigDecimal qty, Long productionCenterId) {
            this.qty = qty;
            this.productionCenterId = productionCenterId;
        }
    }
    
    // Helper class for tracking product usage in hierarchical structure
    private static class UsageTracking {
        Long productId;
        String productName;
        BigDecimal quantityUsed;
        
        UsageTracking(Long productId, String productName, BigDecimal quantityUsed) {
            this.productId = productId;
            this.productName = productName;
            this.quantityUsed = quantityUsed;
        }
    }
    
    // Helper class for direct product usage information
    private static class DirectProductUsage {
        Long productId;
        String productName;
        BigDecimal totalQuantity;
        boolean hasChildProducts;
        
        DirectProductUsage(Long productId, String productName, BigDecimal totalQuantity, boolean hasChildProducts) {
            this.productId = productId;
            this.productName = productName;
            this.totalQuantity = totalQuantity;
            this.hasChildProducts = hasChildProducts;
        }
    }
    
    // Helper class for intermediate product quantity tracking
    private static class IntermediateProductQty {
        BigDecimal intermediateQty;
        BigDecimal parentQtyNeeded;

        IntermediateProductQty(BigDecimal intermediateQty, BigDecimal parentQtyNeeded) {
            this.intermediateQty = intermediateQty;
            this.parentQtyNeeded = parentQtyNeeded;
        }
    }

    // Helper class for final product information
    private static class FinalProductInfo {
        Long productId;
        String productName;
        BigDecimal totalQuantity;
        
        FinalProductInfo(Long productId, String productName, BigDecimal totalQuantity) {
            this.productId = productId;
            this.productName = productName;
            this.totalQuantity = totalQuantity;
        }
    }
    
    // Helper class for aggregating usage details
    private static class UsageAggregate {
        String productName;
        BigDecimal totalQuantity;
        String unit;
        
        UsageAggregate(String productName, BigDecimal totalQuantity, String unit) {
            this.productName = productName;
            this.totalQuantity = totalQuantity;
            this.unit = unit;
        }
    }

    @Override
    public com.plover.backerymanagmentsystem.store_keeper.dto.StorekeeperDashboardStatsResponseDto getDashboardStats() {
        // 1. Pending Manager Requests
        ProductionPlanSummaryResponseDto planSummary = this.getProductionPlanSummary();
        List<ProductionPlanSummaryResponseDto.ProductionPlanDto> pendingManagerRequests = planSummary.getProductionPlans();

        // 2. Recent Transactions (GRNs, Returns, Adjustments)
        List<com.plover.backerymanagmentsystem.store_keeper.dto.StorekeeperDashboardStatsResponseDto.RecentTransactionDto> recentTransactions = new ArrayList<>();

        // GRNs
        com.plover.backerymanagmentsystem.store_keeper.dto.GetAllGrnsResponseDto allGrns = grnService.getAllGrns();
        if (allGrns != null && allGrns.getGrns() != null) {
            allGrns.getGrns().stream()
                .limit(10)
                .forEach(grn -> recentTransactions.add(com.plover.backerymanagmentsystem.store_keeper.dto.StorekeeperDashboardStatsResponseDto.RecentTransactionDto.builder()
                        .id("GRN-" + grn.getGrnId())
                        .type("Goods Received")
                        .supplier(grn.getSupplierName())
                        .date(grn.getReceivedDate() != null ? grn.getReceivedDate().toLocalDate().toString() : "-")
                        .items(grn.getNumberOfItems())
                        .amount("Rs. " + grn.getTotal())
                        .status(grn.getGrnStatus() != null ? grn.getGrnStatus().name() : "N/A")
                        .createdAt(grn.getCreatedAt() != null ? grn.getCreatedAt() : LocalDateTime.now())
                        .build()));
        }

        // Returns
        com.plover.backerymanagmentsystem.store_keeper.dto.GetAllRawMaterialReturnsResponseDto allReturns = rawMaterialReturnService.getAllRawMaterialReturns();
        if (allReturns != null && allReturns.getReturns() != null) {
            allReturns.getReturns().stream()
                .limit(10)
                .forEach(ret -> recentTransactions.add(com.plover.backerymanagmentsystem.store_keeper.dto.StorekeeperDashboardStatsResponseDto.RecentTransactionDto.builder()
                        .id("RTN-" + ret.getReturnId())
                        .type("Material Return")
                        .supplier(ret.getSupplierName())
                        .date(ret.getReturnDate() != null ? ret.getReturnDate().toString() : "-")
                        .items(ret.getNumberOfItems())
                        .amount("Rs. " + ret.getTotalCost())
                        .status("Completed")
                        .createdAt(ret.getCreatedAt() != null ? ret.getCreatedAt() : LocalDateTime.now())
                        .build()));
        }

        // Sort by createdAt desc and take top 5
        List<com.plover.backerymanagmentsystem.store_keeper.dto.StorekeeperDashboardStatsResponseDto.RecentTransactionDto> sortedTransactions = recentTransactions.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

        // 3. Stock Alerts
        List<com.plover.backerymanagmentsystem.store_keeper.dto.LowStockMaterialsResponseDto.LowStockMaterialDto> stockAlerts = lowStockMaterialsService.getLowStockMaterials().getLowStockMaterials();

        // 4. Pending Worker Requests Count
        long pendingWorkerRequestsCount = ingredientRequestRepository.countByStatus(IngredientRequestStatus.PENDING);

        return com.plover.backerymanagmentsystem.store_keeper.dto.StorekeeperDashboardStatsResponseDto.builder()
                .pendingManagerRequests(pendingManagerRequests)
                .recentTransactions(sortedTransactions)
                .stockAlerts(stockAlerts)
                .pendingWorkerRequestsCount(pendingWorkerRequestsCount)
                .build();
    }

    private double getAggregatedCurrentStock(RawMaterial rawMaterial) {
        if (rawMaterial == null) return 0.0;
        String genericName = rawMaterial.getGenericMaterialName();
        List<RawMaterial> allBatches;
        if (genericName != null && !genericName.trim().isEmpty()) {
            allBatches = rawMaterialRepository.findAllByGenericMaterialName(genericName);
        } else if (rawMaterial.getMaterialCode() != null) {
            allBatches = rawMaterialRepository.findAllByMaterialCode(rawMaterial.getMaterialCode());
        } else {
            allBatches = List.of(rawMaterial);
        }
        return allBatches.stream()
                .map(RawMaterial::getCurrentStock)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private static class MaterialQtyCost {
        RawMaterial rawMaterial;
        double qty;
        MaterialQtyCost(RawMaterial rm, double q) {
            this.rawMaterial = rm;
            this.qty = q;
        }
    }

    private Map<Long, MaterialQtyCost> resolveRawMaterialsForProduct(Long productId, Double quantity, java.util.Set<Long> visited) {
        Map<Long, MaterialQtyCost> map = new HashMap<>();
        if (productId == null || quantity == null || quantity <= 0 || visited.contains(productId)) return map;
        visited.add(productId);

        List<BillOfMaterial> bomRows = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(productId);
        if (bomRows != null && !bomRows.isEmpty()) {
            for (BillOfMaterial bom : bomRows) {
                if (bom.getChildType() == BillOfMaterial.ChildType.raw_material) {
                    Long rmId = bom.getChildItemId();
                    RawMaterial rm = rawMaterialRepository.findById(rmId).orElse(null);
                    if (rm != null) {
                        double reqQty = (bom.getQuantity() != null ? bom.getQuantity().doubleValue() : 0.0) * quantity;
                        map.merge(rmId, new MaterialQtyCost(rm, reqQty), (a, b) -> {
                            a.qty += b.qty;
                            return a;
                        });
                    }
                } else if (bom.getChildType() == BillOfMaterial.ChildType.product) {
                    Long childProductId = bom.getChildItemId();
                    double childQty = (bom.getQuantity() != null ? bom.getQuantity().doubleValue() : 0.0) * quantity;
                    Map<Long, MaterialQtyCost> childMap = resolveRawMaterialsForProduct(childProductId, childQty, visited);
                    for (Map.Entry<Long, MaterialQtyCost> entry : childMap.entrySet()) {
                        map.merge(entry.getKey(), entry.getValue(), (a, b) -> {
                            a.qty += b.qty;
                            return a;
                        });
                    }
                }
            }
        }

        if (map.isEmpty()) {
            Optional<Recipe> recipeOpt = recipeRepository.findByProductIdAndIsActiveTrue(productId);
            if (recipeOpt.isPresent() && recipeOpt.get().getRecipeIngredients() != null) {
                for (RecipeIngredient ri : recipeOpt.get().getRecipeIngredients()) {
                    if (ri.getRawMaterial() != null) {
                        Long rmId = ri.getRawMaterial().getId();
                        double reqQty = (ri.getQuantityPerUnit() != null ? ri.getQuantityPerUnit() : 0.0) * quantity;
                        map.merge(rmId, new MaterialQtyCost(ri.getRawMaterial(), reqQty), (a, b) -> {
                            a.qty += b.qty;
                            return a;
                        });
                    }
                }
            }
        }
        visited.remove(productId);
        return map;
    }
}
