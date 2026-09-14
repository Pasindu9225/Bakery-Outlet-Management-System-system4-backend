package com.plover.backerymanagmentsystem.manager.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.dto.OutletAllocationRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanItemRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanItemResponseDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanResponseDto;
import com.plover.backerymanagmentsystem.manager.dto.RawMaterialRequirementDto;
import com.plover.backerymanagmentsystem.manager.dto.EnhancedProductionPlanResponseDto;
import com.plover.backerymanagmentsystem.manager.dto.DistributionPlanDto;
import com.plover.backerymanagmentsystem.manager.dto.DistributionPlanItemDto;
import com.plover.backerymanagmentsystem.manager.model.DistributionPlan;
import com.plover.backerymanagmentsystem.manager.model.DistributionPlanItem;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;
import com.plover.backerymanagmentsystem.manager.model.ProductionPlanMaterials;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.DistributionPlanItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.DistributionPlanRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanMaterialsRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeIngredientRepository;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository;
import com.plover.backerymanagmentsystem.manager.dto.ReservationResult;
import com.plover.backerymanagmentsystem.manager.service.SemiFinishedReservationService;
import com.plover.backerymanagmentsystem.manager.service.ProductionPlanningService;
import com.plover.backerymanagmentsystem.store_keeper.service.BomService;
import com.plover.backerymanagmentsystem.store_keeper.dto.BomTreeResponseDto;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;
import com.plover.backerymanagmentsystem.notification.service.NotificationService;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequest;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequestItem;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus;
import com.plover.backerymanagmentsystem.manager.repository.IngredientRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductionPlanningServiceImpl implements ProductionPlanningService {

    private final ProductionPlanRepository productionPlanRepository;
    private final ProductionPlanItemRepository productionPlanItemRepository;
    private final ProductRepository productRepository;
    private final RecipeRepository recipeRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final ProductionPlanMaterialsRepository productionPlanMaterialsRepository;
    private final MiniStoreItemRepository miniStoreItemRepository;
    private final DistributionPlanRepository distributionPlanRepository;
    private final DistributionPlanItemRepository distributionPlanItemRepository;
    private final OutletRepository outletRepository;
    private final BomService bomService;
    private final BillOfMaterialRepository billOfMaterialRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final NotificationService notificationService;
    private final IngredientRequestRepository ingredientRequestRepository;
    private final SemiFinishedReservationService semiFinishedReservationService;

    @jakarta.annotation.PostConstruct
    public void autoApproveExistingPendingPlans() {
        try {
            List<ProductionPlan> plans = productionPlanRepository.findAll();
            int updatedCount = 0;
            for (ProductionPlan plan : plans) {
                if (plan.getStatus() == ProductionPlan.ProductionPlanStatus.DRAFT ||
                    plan.getStatus() == ProductionPlan.ProductionPlanStatus.SUBMITTED) {
                    plan.setStatus(ProductionPlan.ProductionPlanStatus.APPROVED);
                    productionPlanRepository.save(plan);
                    updatedCount++;
                }
            }
            if (updatedCount > 0) {
                log.info("Auto-approved {} legacy production plans in database.", updatedCount);
            }
        } catch (Exception e) {
            log.warn("Auto-approve existing plans check failed: {}", e.getMessage());
        }
    }

    @Override
    public ProductionPlanResponseDto createProductionPlan(ProductionPlanRequestDto requestDto) {
        log.info("Creating production plan: {}", requestDto.getPlanName());

        // Create production plan (Always set status to APPROVED so plans are immediately visible and actionable across modules)
        ProductionPlan.ProductionPlanStatus initialStatus = ProductionPlan.ProductionPlanStatus.APPROVED;
        if (requestDto.getStatus() != null && "CANCELLED".equalsIgnoreCase(requestDto.getStatus())) {
            initialStatus = ProductionPlan.ProductionPlanStatus.CANCELLED;
        }

        ProductionPlan productionPlan = ProductionPlan.builder()
                .planName(requestDto.getPlanName())
                .planDate(requestDto.getPlanDate() != null ? requestDto.getPlanDate().toLocalDateTime() : null)
                .status(initialStatus)
                .notes(requestDto.getNotes())
                .department(requestDto.getDepartment())
                .isTemplate(requestDto.getIsTemplate() != null ? requestDto.getIsTemplate() : false)
                .build();

        // Create production plan items
        List<ProductionPlanItem> planItems = new ArrayList<>();
        double totalEstimatedCost = 0.0;

        for (ProductionPlanItemRequestDto itemDto : requestDto.getProductionItems()) {
            ProductionCenter productionCenter = null;
            if (itemDto.getProductionCenterId() != null) {
                Long centerId = itemDto.getProductionCenterId();
                productionCenter = productionCenterRepository.findById(centerId).orElse(null);
                
                if (productionCenter == null) {
                    log.warn("Production center with ID {} not found. Falling back to default.", centerId);
                }
            }

            if (productionCenter == null) {
                // If not provided or not found, use the first available production center as default
                productionCenter = productionCenterRepository.findActiveAndEstablished().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("CRITICAL ERROR: No active, established production centers found in the database. Please add an established production center before creating a plan."));
            }

            ProductionPlanItem planItem;
            
            if ("product".equals(itemDto.getType())) {
                // Handle product item
                if (itemDto.getProductId() == null) {
                    throw new RuntimeException("Product ID is required when type is 'product'");
                }
                
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + itemDto.getProductId()));

                Long resolvedCenterId = product.getProductionCenterId();
                if (resolvedCenterId == null) {
                    resolvedCenterId = productionCenter.getId();
                }

                double dynamicUnitCost = calculateFifoProductUnitCost(product.getId(), itemDto.getQuantity());

                planItem = ProductionPlanItem.builder()
                        .productionPlan(productionPlan)
                        .productId(itemDto.getProductId())
                        .rawMaterialId(null) // Set to null for products
                        .productName(itemDto.getProductName())
                        .quantity(itemDto.getQuantity())
                        .unitCost(dynamicUnitCost)
                        .totalCost(dynamicUnitCost * itemDto.getQuantity())
                        .productionCenterId(resolvedCenterId)
                        .isTopLevel(true)
                        .build();
                        
            } else if ("raw_material".equals(itemDto.getType())) {
                // Handle raw material item
                // When type is raw_material, productId from request should be treated as rawMaterialId
                Long requestedRawMaterialId = itemDto.getRawMaterialId();
                Long requestedProductId = itemDto.getProductId();
                
                // Determine which ID to use for raw material
                final Long rawMaterialIdToUse = (requestedRawMaterialId != null) 
                    ? requestedRawMaterialId 
                    : requestedProductId;
                
                if (rawMaterialIdToUse == null) {
                    throw new RuntimeException("Raw Material ID is required when type is 'raw_material'");
                }
                
                RawMaterial rawMaterial = rawMaterialRepository.findById(rawMaterialIdToUse)
                        .orElseThrow(() -> new RuntimeException("Raw material not found: " + rawMaterialIdToUse));

                planItem = ProductionPlanItem.builder()
                        .productionPlan(productionPlan)
                        .productId(null) // Set to null for raw materials
                        .rawMaterialId(rawMaterialIdToUse)
                        .productName(itemDto.getProductName())
                        .quantity(itemDto.getQuantity())
                        .unitCost(rawMaterial.getUnitCost())
                        .totalCost(rawMaterial.getUnitCost() * itemDto.getQuantity())
                        .productionCenterId(productionCenter.getId())
                        .isTopLevel(true)
                        .build();
            } else {
                throw new RuntimeException("Invalid type: " + itemDto.getType() + ". Must be 'product' or 'raw_material'");
            }

            planItems.add(planItem);
            totalEstimatedCost += planItem.getTotalCost();
        }

        productionPlan.setProductionPlanItems(planItems);
        productionPlan.setTotalEstimatedCost(totalEstimatedCost);

        ProductionPlan savedPlan = productionPlanRepository.save(productionPlan);

        List<ProductionPlanItem> childItems = new ArrayList<>();
        java.util.Set<Long> visited = new java.util.HashSet<>();
        for (ProductionPlanItem planItem : savedPlan.getProductionPlanItems()) {
            if (planItem.getProductId() != null && Boolean.TRUE.equals(planItem.getIsTopLevel())) {
                expandChildProducts(planItem, planItem.getQuantity().doubleValue(), 
                        planItem.getProductionCenterId(), childItems, savedPlan, visited);
            }
        }
        if (!childItems.isEmpty()) {
            savedPlan.getProductionPlanItems().addAll(childItems);
            savedPlan = productionPlanRepository.save(savedPlan);
        }
 
        // Calculate raw material requirements
        List<RawMaterialRequirementDto> rawMaterialRequirements = calculateRawMaterialRequirements(savedPlan.getId());
        Double totalRawMaterialCost = calculateTotalRawMaterialCost(savedPlan.getId());
 
        // Persist center-wise raw material requirements
        saveProductionPlanMaterials(savedPlan.getId(), rawMaterialRequirements);
 
        // Update the plan (do not double count raw materials by adding totalRawMaterialCost)
        savedPlan.setTotalEstimatedCost(totalEstimatedCost);
        productionPlanRepository.save(savedPlan);
 
        // Generate IngredientRequests if plan is APPROVED/SUBMITTED
        generateIngredientRequestsIfApproved(savedPlan, rawMaterialRequirements);

        // Create distribution plans and items per outlet/date from request allocations
        createDistributionPlansFromAllocations(requestDto, savedPlan);
 
        // Send notification to Manager
        notificationService.sendNotification(
            "New Production Plan Created",
            "Plan '" + savedPlan.getPlanName() + "' has been created successfully.",
            "success",
            "10" // Manager role
        );

        // Also notify Bakery/Kitchen workers if relevant
        boolean hasBakeryItems = false;
        boolean hasKitchenItems = false;

        String planNameLower = savedPlan.getPlanName() != null ? savedPlan.getPlanName().toLowerCase() : "";
        if (planNameLower.contains("bakery")) {
            hasBakeryItems = true;
        }
        if (planNameLower.contains("kitchen")) {
            hasKitchenItems = true;
        }

        if (savedPlan.getProductionPlanItems() != null) {
            for (ProductionPlanItem item : savedPlan.getProductionPlanItems()) {
                if (item.getProductionCenterId() != null) {
                    try {
                        ProductionCenter center = productionCenterRepository.findById(item.getProductionCenterId()).orElse(null);
                        if (center != null && center.getType() != null) {
                            if (center.getType() == ProductionCenterType.BAKERY) {
                                hasBakeryItems = true;
                            } else if (center.getType() == ProductionCenterType.KITCHEN) {
                                hasKitchenItems = true;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error finding production center for notification logic: {}", e.getMessage());
                    }
                }
            }
        }

        if (hasBakeryItems) {
            notificationService.sendNotification(
                "New Bakery Production Assigned",
                "A new production plan '" + savedPlan.getPlanName() + "' is ready for processing.",
                "info",
                "12" // Bakery Worker
            );
        }
        if (hasKitchenItems) {
            notificationService.sendNotification(
                "New Kitchen Production Assigned",
                "A new production plan '" + savedPlan.getPlanName() + "' is ready for processing.",
                "info",
                "13" // Kitchen Worker
            );
        }

        return buildProductionPlanResponse(savedPlan, rawMaterialRequirements, totalRawMaterialCost);
    }

    @Override
    public EnhancedProductionPlanResponseDto createProductionPlanWithBom(ProductionPlanRequestDto requestDto) {
        log.info("Creating production plan with BOM: {}", requestDto.getPlanName());

        // First, create the production plan using existing logic
        ProductionPlanResponseDto basicResponse = createProductionPlan(requestDto);
        
        // Now enhance it with BOM tree data
        return buildEnhancedProductionPlanResponse(basicResponse, requestDto);
    }

    private EnhancedProductionPlanResponseDto buildEnhancedProductionPlanResponse(
            ProductionPlanResponseDto basicResponse, ProductionPlanRequestDto requestDto) {
        
        List<EnhancedProductionPlanResponseDto.ProductWithBomDto> productsWithBom = new ArrayList<>();
        
        for (ProductionPlanItemRequestDto item : requestDto.getProductionItems()) {
            if ("product".equals(item.getType())) {
                // Handle product with BOM tree
                BomTreeResponseDto bomTree = bomService.getBomTree(item.getProductId());
                
                // Scale quantities and add costs
                List<EnhancedProductionPlanResponseDto.BomNodeDto> scaledChildren = 
                    scaleBomTreeQuantities(bomTree.getChildren(), BigDecimal.valueOf(item.getQuantity()));
                
                EnhancedProductionPlanResponseDto.ProductWithBomDto productWithBom = 
                    EnhancedProductionPlanResponseDto.ProductWithBomDto.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .plannedQuantity(item.getQuantity())
                        .children(scaledChildren)
                        .build();
                
                productsWithBom.add(productWithBom);
            } else if ("raw_material".equals(item.getType())) {
                // Handle raw material directly (no BOM tree)
                // When type is raw_material, productId from request should be treated as rawMaterialId
                Long requestedRawMaterialId = item.getRawMaterialId();
                Long requestedProductId = item.getProductId();
                
                // Determine which ID to use for raw material
                final Long rawMaterialIdToUse = (requestedRawMaterialId != null) 
                    ? requestedRawMaterialId 
                    : requestedProductId;
                
                RawMaterial rawMaterial = rawMaterialRepository.findById(rawMaterialIdToUse)
                        .orElseThrow(() -> new RuntimeException("Raw material not found: " + rawMaterialIdToUse));
                
                // Create a single BOM node for the raw material
                EnhancedProductionPlanResponseDto.BomNodeDto rawMaterialNode = 
                    EnhancedProductionPlanResponseDto.BomNodeDto.builder()
                        .childItemId(rawMaterial.getId())
                        .childName(rawMaterial.getMaterialName())
                        .childType("raw_material")
                        .quantity(java.math.BigDecimal.valueOf(item.getQuantity()))
                        .unit(rawMaterial.getUnitOfMeasure())
                        .unitCost(java.math.BigDecimal.valueOf(rawMaterial.getUnitCost()))
                        .totalCost(java.math.BigDecimal.valueOf(rawMaterial.getUnitCost()).multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                        .children(new ArrayList<>()) // Raw materials don't have children
                        .build();
                
                EnhancedProductionPlanResponseDto.ProductWithBomDto rawMaterialWithBom = 
                    EnhancedProductionPlanResponseDto.ProductWithBomDto.builder()
                        .productId(null) // No product ID for raw materials
                        .productName(item.getProductName())
                        .plannedQuantity(item.getQuantity())
                        .children(List.of(rawMaterialNode)) // Single raw material node
                        .build();
                
                productsWithBom.add(rawMaterialWithBom);
            }
        }
        
        return EnhancedProductionPlanResponseDto.builder()
                .planId(basicResponse.getId())
                .planName(basicResponse.getPlanName())
                .planDate(basicResponse.getPlanDate())
                .status(basicResponse.getStatus())
                .createdBy("Manager") // You can get this from user context
                .remarks(basicResponse.getNotes())
                .totalEstimatedCost(basicResponse.getTotalEstimatedCost())
                .totalRawMaterialCost(basicResponse.getTotalRawMaterialCost())
                .productionItems(basicResponse.getProductionItems())
                .rawMaterialRequirements(basicResponse.getRawMaterialRequirements())
                .products(productsWithBom)
                .build();
    }

    private List<EnhancedProductionPlanResponseDto.BomNodeDto> scaleBomTreeQuantities(
            List<BomTreeResponseDto.BomNodeDto> bomNodes, BigDecimal currentMultiplier) {
        
        return bomNodes.stream().map(node -> {
            // Scale the quantity using the accumulated parent multiplier
            BigDecimal scaledQuantity = node.getQuantity().multiply(currentMultiplier);
            
            // Calculate costs for raw materials
            BigDecimal unitCost = null;
            BigDecimal totalCost = null;
            
            if ("raw_material".equals(node.getChildType())) {
                // Fetch raw material to get unit cost
                RawMaterial rawMaterial = rawMaterialRepository.findById(node.getChildItemId()).orElse(null);
                if (rawMaterial != null) {
                    unitCost = BigDecimal.valueOf(rawMaterial.getUnitCost());
                    totalCost = scaledQuantity.multiply(unitCost);
                }
            }
            
            // Recursively scale children passing the node's scaledQuantity as multiplier
            List<EnhancedProductionPlanResponseDto.BomNodeDto> scaledChildren = 
                node.getChildren() != null ? scaleBomTreeQuantities(node.getChildren(), scaledQuantity) : new ArrayList<>();
            
            return EnhancedProductionPlanResponseDto.BomNodeDto.builder()
                    .childItemId(node.getChildItemId())
                    .childName(node.getChildName())
                    .childType(node.getChildType())
                    .quantity(scaledQuantity)
                    .unit(node.getUnit())
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .productionCenter(node.getProductionCenter())
                    .children(scaledChildren)
                    .build();
        }).collect(Collectors.toList());
    }

    private void createDistributionPlansFromAllocations(ProductionPlanRequestDto requestDto, ProductionPlan productionPlan) {
        if (requestDto.getProductionItems() == null) {
            return;
        }

        log.info("Processing {} items for potential distribution plans", requestDto.getProductionItems().size());

        // Group DistributionPlan by (outletId or outletName, date)
        Map<String, DistributionPlan> planByOutletAndDate = new HashMap<>();

        for (ProductionPlanItemRequestDto item : requestDto.getProductionItems()) {
            if (item.getOutlets() == null || item.getOutlets().isEmpty()) {
                continue;
            }

            // Distribution plans currently only support products
            if (!"product".equals(item.getType()) || item.getProductId() == null) {
                log.warn("Skipping distribution for item '{}' as it is not a product or has no ID", item.getProductName());
                continue;
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found for distribution: " + item.getProductId()));

            for (OutletAllocationRequestDto alloc : item.getOutlets()) {
                if (alloc.getQuantity() == null || alloc.getQuantity() <= 0) {
                    log.debug("Skipping zero/null quantity allocation for product {} to outlet {}", 
                            item.getProductName(), alloc.getOutletId());
                    continue;
                }

                // Use provided allocation date or fall back to main plan date
                java.time.LocalDate distDate = alloc.getDate();
                if (distDate == null && requestDto.getPlanDate() != null) {
                    distDate = requestDto.getPlanDate().toLocalDate();
                }

                if (distDate == null) {
                    log.warn("Skipping allocation for product {} to outlet {} because no date could be determined", 
                            item.getProductName(), alloc.getOutletId());
                    continue;
                }

                Outlet outlet = resolveOutlet(alloc);
                String key = (outlet.getOutletId() != null ? outlet.getOutletId().toString() : outlet.getName())
                        + "|" + distDate.toString();

                DistributionPlan dp = planByOutletAndDate.get(key);
                if (dp == null) {
                    // Default status to NOT_RECEIVED for new plans
                    com.plover.backerymanagmentsystem.manager.model.DistributionPlanStatus initialStatus = 
                            com.plover.backerymanagmentsystem.manager.model.DistributionPlanStatus.NOT_RECEIVED;
                    
                    dp = DistributionPlan.builder()
                            .name(requestDto.getPlanName())
                            .outlet(outlet)
                            .date(distDate)
                            .isActive(alloc.getIsActive() != null ? alloc.getIsActive() : true)
                            .status(initialStatus)
                            .productionPlan(productionPlan)
                            .build();
                    dp = distributionPlanRepository.save(dp);
                    planByOutletAndDate.put(key, dp);
                }

                DistributionPlanItem dpi = DistributionPlanItem.builder()
                        .distributionPlan(dp)
                        .product(product)
                        .qty(alloc.getQuantity())
                        .build();
                distributionPlanItemRepository.save(dpi);
            }
        }
    }

    private Outlet resolveOutlet(OutletAllocationRequestDto alloc) {
        Outlet outlet = null;
        if (alloc.getOutletId() != null) {
            outlet = outletRepository.findById(alloc.getOutletId())
                    .orElse(null);
        }
        if (outlet == null) {
            String name = alloc.getOutletName();
            if (name == null || name.isBlank()) {
                throw new RuntimeException("Outlet information missing (id or name) in allocation");
            }
            // Attempt to split "name - address"
            int idx = name.indexOf(" - ");
            final String actualName = (idx > 0) ? name.substring(0, idx).trim() : name;
            final String actualAddress = (idx > 0) ? name.substring(idx + 3).trim() : null;

            // Search by name before creating a new one
            outlet = outletRepository.findAll().stream()
                    .filter(o -> actualName.equalsIgnoreCase(o.getName()))
                    .findFirst()
                    .orElseGet(() -> {
                        Outlet newOutlet = Outlet.builder()
                                .name(actualName)
                                .address(actualAddress)
                                .build();
                        return outletRepository.save(newOutlet);
                    });
        }
        return outlet;
    }

    private void saveProductionPlanMaterials(Long planId, List<RawMaterialRequirementDto> requirements) {
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + planId));

        // Clear previous entries for idempotency
        productionPlanMaterialsRepository.deleteByProductionPlan_Id(planId);

        // Group by raw material and split quantities by center
        Map<Long, ProductionPlanMaterials> byMaterial = new HashMap<>();

        for (RawMaterialRequirementDto req : requirements) {
            Long materialId = req.getRawMaterialId();
            ProductionPlanMaterials row = byMaterial.computeIfAbsent(materialId, id -> {
                RawMaterial material = rawMaterialRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Raw material not found: " + id));
                return ProductionPlanMaterials.builder()
                        .productionPlan(plan)
                        .rawMaterial(material)
                        .rawMaterialQuantity(0.0)
                        .rawMaterialQuantityForKitchen(null)
                        .rawMaterialQuantityForBakery(null)
                        .build();
            });

            Double qty = req.getRequiredQuantity() == null ? 0.0 : req.getRequiredQuantity();
            row.setRawMaterialQuantity((row.getRawMaterialQuantity() == null ? 0.0 : row.getRawMaterialQuantity()) + qty);

            String center = req.getProductionCenterName();
            if (center != null) {
                String normalized = center.trim().toLowerCase();
                if (normalized.contains("kitchen")) {
                    row.setRawMaterialQuantityForKitchen((row.getRawMaterialQuantityForKitchen() == null ? 0.0 : row.getRawMaterialQuantityForKitchen()) + qty);
                } else if (normalized.contains("bakery")) {
                    row.setRawMaterialQuantityForBakery((row.getRawMaterialQuantityForBakery() == null ? 0.0 : row.getRawMaterialQuantityForBakery()) + qty);
                }
            }
        }

        productionPlanMaterialsRepository.saveAll(byMaterial.values());
    }

    @Override
    public ProductionPlanResponseDto getProductionPlanById(Long planId) {
        ProductionPlan productionPlan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + planId));

        List<RawMaterialRequirementDto> rawMaterialRequirements = calculateRawMaterialRequirements(planId);
        Double totalRawMaterialCost = calculateTotalRawMaterialCost(planId);

        return buildProductionPlanResponse(productionPlan, rawMaterialRequirements, totalRawMaterialCost);
    }

    @Override
    public List<ProductionPlanResponseDto> getAllProductionPlans() {
        List<ProductionPlan> plans = productionPlanRepository.findByOrderByCreatedAtDesc();
        return plans.stream()
                .map(plan -> {
                    List<RawMaterialRequirementDto> rawMaterialRequirements = calculateRawMaterialRequirements(plan.getId());
                    Double totalRawMaterialCost = calculateTotalRawMaterialCost(plan.getId());
                    return buildProductionPlanResponse(plan, rawMaterialRequirements, totalRawMaterialCost);
                })
                .collect(Collectors.toList());
    }

    @Override
    public ProductionPlanResponseDto updateProductionPlanStatus(Long planId, String status) {
        ProductionPlan productionPlan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + planId));

        try {
            ProductionPlan.ProductionPlanStatus newStatus = ProductionPlan.ProductionPlanStatus.valueOf(status.toUpperCase());
            productionPlan.setStatus(newStatus);
            ProductionPlan savedPlan = productionPlanRepository.save(productionPlan);

            List<RawMaterialRequirementDto> rawMaterialRequirements = calculateRawMaterialRequirements(planId);
            Double totalRawMaterialCost = calculateTotalRawMaterialCost(planId);

            generateIngredientRequestsIfApproved(savedPlan, rawMaterialRequirements);

            return buildProductionPlanResponse(savedPlan, rawMaterialRequirements, totalRawMaterialCost);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
    }

    // @Override
    // public List<RawMaterialRequirementDto> calculateRawMaterialRequirements(Long planId) {
    //     ProductionPlan productionPlan = productionPlanRepository.findById(planId)
    //             .orElseThrow(() -> new RuntimeException("Production plan not found: " + planId));
    // Create New Table for production Center and get a foregin key for Product Item, and filter and respond based on that
    //     Map<Long, Double> totalRawMaterialRequirements = new HashMap<>();
    //     Map<Long, RawMaterial> rawMaterialMap = new HashMap<>();
    //     Map<ProductionCenter, Double> productionCenterItems = new HashMap<>();
    //     List<ProductionCenter> productionCenters = productionCenterRepository.findAll();
    //     // Calculate raw material requirements for each product
    //     for (ProductionPlanItem planItem : productionPlan.getProductionPlanItems()) {
    //         Recipe recipe = recipeRepository.findByProductIdAndIsActiveTrue(planItem.getProductId())
    //                 .orElseThrow(() -> new RuntimeException("Recipe not found for product: " + planItem.getProductName()));
    //         // Calculate raw material requirements for this product quantity
    //         for (RecipeIngredient ingredient : recipe.getRecipeIngredients()) {
    //             Long materialId = ingredient.getRawMaterial().getId();
    //             Double requiredQuantity = ingredient.getQuantityPerUnit() * planItem.getQuantity();
    //             totalRawMaterialRequirements.merge(materialId, requiredQuantity, Double::sum);
    //             rawMaterialMap.put(materialId, ingredient.getRawMaterial());
    //         }
    //     }
    //     // Build response DTOs
    //     List<RawMaterialRequirementDto> requirements = new ArrayList<>();
    //     for (Map.Entry<Long, Double> entry : totalRawMaterialRequirements.entrySet()) {
    //         Long materialId = entry.getKey();
    //         Double requiredQuantity = entry.getValue();
    //         RawMaterial rawMaterial = rawMaterialMap.get(materialId);
    //         String productionCenterName= planItem.getProductionCenter().getCenterName();
    //         Double totalCost = requiredQuantity * rawMaterial.getUnitCost();
    //         Double stockDeficit = Math.max(0, requiredQuantity - rawMaterial.getCurrentStock());
    //         RawMaterialRequirementDto requirementDto = RawMaterialRequirementDto.builder()
    //                 .rawMaterialId(materialId)
    //                 .rawMaterialName(rawMaterial.getMaterialName())
    //                 .materialCode(rawMaterial.getMaterialCode())
    //                 .requiredQuantity(roundToTwoDecimals(requiredQuantity))
    //                 .unitOfMeasure(rawMaterial.getUnitOfMeasure())
    //                 .unitCost(roundToTwoDecimals(rawMaterial.getUnitCost()))
    //                 .totalCost(roundToTwoDecimals(totalCost))
    //                 .availableStock(roundToTwoDecimals(rawMaterial.getCurrentStock()))
    //                 .stockDeficit(roundToTwoDecimals(stockDeficit))
    //                 .build();
    //         requirements.add(requirementDto);
    //     }
    //     return requirements;
    // }
    @Override
    public List<RawMaterialRequirementDto> calculateRawMaterialRequirements(Long planId) {
        // 1. Get production plan
        ProductionPlan productionPlan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + planId));

        // 2. Keep track of requirements grouped by production center
        Map<ProductionCenter, Map<Long, Double>> centerWiseRequirements = new HashMap<>();
        Map<Long, RawMaterial> rawMaterialMap = new HashMap<>();

        // To prevent infinite recursion in circular BOMs
        java.util.Set<Long> visitedProducts = new java.util.HashSet<>();

        // 3. Loop through each top-level item in the plan and calculate its full raw material requirements
        for (ProductionPlanItem planItem : productionPlan.getProductionPlanItems()) {
            boolean isTop = Boolean.TRUE.equals(planItem.getIsTopLevel()) || planItem.getParentPlanItemId() == null;
            if (!isTop) {
                // Skip child sub-assemblies to prevent double-counting as top-level product recursively expands them
                continue;
            }

            ProductionCenter center = null;
            if (planItem.getProductionCenterId() != null) {
                center = productionCenterRepository.findById(planItem.getProductionCenterId()).orElse(null);
            }
            if (center == null && planItem.getProductId() != null) {
                Product p = productRepository.findById(planItem.getProductId()).orElse(null);
                if (p != null && p.getProductionCenterId() != null) {
                    center = productionCenterRepository.findById(p.getProductionCenterId()).orElse(null);
                }
            }
            
            double effQty = planItem.getExactQuantity() != null && planItem.getExactQuantity() > 0 
                    ? planItem.getExactQuantity() 
                    : (planItem.getQuantity() != null ? planItem.getQuantity().doubleValue() : 0.0);
            
            if (effQty <= 0.00001) {
                continue;
            }

            if (planItem.getProductId() != null) {
                visitedProducts.clear();
                calculateRawMaterialRequirementsForProduct(planItem.getProductId(), effQty, 
                        center, centerWiseRequirements, rawMaterialMap, visitedProducts, true);
            } else if (planItem.getRawMaterialId() != null) {
                Long materialId = planItem.getRawMaterialId();
                Double requiredQuantity = effQty;
                
                RawMaterial rawMaterial = rawMaterialRepository.findById(materialId).orElse(null);
                if (rawMaterial != null) {
                    if (!rawMaterialMap.containsKey(materialId)) {
                        rawMaterialMap.put(materialId, rawMaterial);
                    }
                    centerWiseRequirements
                            .computeIfAbsent(center, k -> new HashMap<>())
                            .merge(materialId, requiredQuantity, Double::sum);
                }
            }
        }

        // 4. Build response DTOs
        List<RawMaterialRequirementDto> requirements = new ArrayList<>();
        for (Map.Entry<ProductionCenter, Map<Long, Double>> centerEntry : centerWiseRequirements.entrySet()) {
            ProductionCenter center = centerEntry.getKey();
            String productionCenterName = center != null ? center.getCenterName() : "Unknown Center";

            for (Map.Entry<Long, Double> materialEntry : centerEntry.getValue().entrySet()) {
                Long materialId = materialEntry.getKey();
                Double requiredQuantity = materialEntry.getValue();
                RawMaterial rawMaterial = rawMaterialMap.get(materialId);

                 if (rawMaterial != null) {
                    double totalAvailable = 0.0;
                    String genericName = rawMaterial.getGenericMaterialName();
                    List<RawMaterial> allBatches;
                    if (genericName != null && !genericName.trim().isEmpty()) {
                        allBatches = rawMaterialRepository.findAllByGenericMaterialName(genericName);
                    } else if (rawMaterial.getMaterialCode() != null) {
                        allBatches = rawMaterialRepository.findAllByMaterialCode(rawMaterial.getMaterialCode());
                    } else {
                        allBatches = List.of(rawMaterial);
                    }

                    double physicalStock = allBatches.stream()
                            .map(RawMaterial::getCurrentStock)
                            .filter(java.util.Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .sum();

                    double reservedStock = getReservedStockForMaterial(rawMaterial, planId);
                    totalAvailable = Math.max(0.0, physicalStock - reservedStock);

                    Double totalCost = calculateFifoCostForMaterial(rawMaterial, requiredQuantity);
                    Double stockDeficit = Math.max(0, requiredQuantity - totalAvailable);
                    Double weightedUnitCost = requiredQuantity > 0 ? (totalCost / requiredQuantity) : (rawMaterial.getUnitCost() != null ? rawMaterial.getUnitCost() : 0.0);
                    String displayName = rawMaterial.getDisplayName();

                    requirements.add(RawMaterialRequirementDto.builder()
                            .rawMaterialId(materialId)
                            .rawMaterialName(displayName)
                            .materialCode(rawMaterial.getMaterialCode())
                            .requiredQuantity(roundToThreeDecimals(requiredQuantity))
                            .unitOfMeasure(rawMaterial.getUnitOfMeasure())
                            .unitCost(roundToTwoDecimals(weightedUnitCost))
                            .totalCost(roundToTwoDecimals(totalCost))
                            .availableStock(roundToThreeDecimals(totalAvailable))
                            .stockDeficit(roundToThreeDecimals(stockDeficit))
                            .productionCenterName(productionCenterName)
                            .productionCenterId(center != null ? center.getId() : null)
                            .build());
                }
            }
        }

        return requirements;
    }

    private double getReservedStockForMaterial(RawMaterial rawMaterial, Long excludePlanId) {
        if (rawMaterial == null) return 0.0;
        
        List<ProductionPlan> activePlans = productionPlanRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProductionPlan.ProductionPlanStatus.APPROVED || 
                             p.getStatus() == ProductionPlan.ProductionPlanStatus.IN_PROGRESS)
                .filter(p -> excludePlanId == null || !p.getId().equals(excludePlanId))
                .collect(Collectors.toList());

        if (activePlans.isEmpty()) {
            return 0.0;
        }

        double reservedSum = 0.0;
        String targetCode = rawMaterial.getMaterialCode();
        String targetGeneric = rawMaterial.getGenericMaterialName();

        for (ProductionPlan plan : activePlans) {
            List<IngredientRequest> requests = ingredientRequestRepository.findByProductionPlanId(plan.getId());
            boolean allIssued = !requests.isEmpty() && requests.stream().allMatch(r -> 
                    r.getStatus() == IngredientRequestStatus.ISSUED || 
                    r.getStatus() == IngredientRequestStatus.RECEIVED);

            if (allIssued) {
                continue;
            }

            List<ProductionPlanMaterials> planMaterials = productionPlanMaterialsRepository.findByProductionPlan_Id(plan.getId());
            for (ProductionPlanMaterials ppm : planMaterials) {
                RawMaterial mat = ppm.getRawMaterial();
                if (mat != null) {
                    boolean matches = (targetCode != null && targetCode.equalsIgnoreCase(mat.getMaterialCode())) ||
                                      (targetGeneric != null && !targetGeneric.isBlank() && targetGeneric.equalsIgnoreCase(mat.getGenericMaterialName())) ||
                                      rawMaterial.getId().equals(mat.getId());
                    if (matches) {
                        Double qty = ppm.getRawMaterialQuantity();
                        if (qty != null && qty > 0) {
                            reservedSum += qty;
                        }
                    }
                }
            }
        }

        return reservedSum;
    }

    @Override
    public com.plover.backerymanagmentsystem.manager.dto.EnhancedProductionPlanResponseDto previewProductionPlanMaterials(ProductionPlanRequestDto requestDto) {
        Map<ProductionCenter, Map<Long, Double>> centerWiseRequirements = new HashMap<>();
        Map<Long, RawMaterial> rawMaterialMap = new HashMap<>();
        java.util.Set<Long> visitedProducts = new java.util.HashSet<>();

        for (ProductionPlanItemRequestDto itemDto : requestDto.getProductionItems()) {
            ProductionCenter center = null;
            if (itemDto.getProductionCenterId() != null) {
                center = productionCenterRepository.findById(itemDto.getProductionCenterId()).orElse(null);
            }
            if (center == null && itemDto.getProductId() != null) {
                Product p = productRepository.findById(itemDto.getProductId()).orElse(null);
                if (p != null && p.getProductionCenterId() != null) {
                    center = productionCenterRepository.findById(p.getProductionCenterId()).orElse(null);
                }
            }
            
            if ("product".equals(itemDto.getType()) && itemDto.getProductId() != null) {
                visitedProducts.clear();
                calculateRawMaterialRequirementsForProduct(itemDto.getProductId(), itemDto.getQuantity().doubleValue(), 
                        center, centerWiseRequirements, rawMaterialMap, visitedProducts);
            } else if ("raw_material".equals(itemDto.getType()) || itemDto.getRawMaterialId() != null || itemDto.getProductId() != null) {
                Long materialId = itemDto.getRawMaterialId() != null ? itemDto.getRawMaterialId() : itemDto.getProductId();
                if (materialId != null) {
                    Double requiredQuantity = itemDto.getQuantity().doubleValue();
                    RawMaterial rawMaterial = rawMaterialRepository.findById(materialId).orElse(null);
                    if (rawMaterial != null) {
                        if (!rawMaterialMap.containsKey(materialId)) {
                            rawMaterialMap.put(materialId, rawMaterial);
                        }
                        centerWiseRequirements
                                .computeIfAbsent(center, k -> new HashMap<>())
                                .merge(materialId, requiredQuantity, Double::sum);
                    }
                }
            }
        }

        List<RawMaterialRequirementDto> requirements = new ArrayList<>();
        double totalRawMaterialCost = 0.0;

        for (Map.Entry<ProductionCenter, Map<Long, Double>> centerEntry : centerWiseRequirements.entrySet()) {
            ProductionCenter center = centerEntry.getKey();
            String productionCenterName = center != null ? center.getCenterName() : "Unknown Center";

            for (Map.Entry<Long, Double> materialEntry : centerEntry.getValue().entrySet()) {
                Long materialId = materialEntry.getKey();
                Double requiredQuantity = materialEntry.getValue();
                RawMaterial rawMaterial = rawMaterialMap.get(materialId);

                if (rawMaterial != null) {
                    String genericName = rawMaterial.getGenericMaterialName();
                    List<RawMaterial> allBatches;
                    if (genericName != null && !genericName.trim().isEmpty()) {
                        allBatches = rawMaterialRepository.findAllByGenericMaterialName(genericName);
                    } else if (rawMaterial.getMaterialCode() != null) {
                        allBatches = rawMaterialRepository.findAllByMaterialCode(rawMaterial.getMaterialCode());
                    } else {
                        allBatches = List.of(rawMaterial);
                    }

                    double physicalStock = allBatches.stream()
                            .map(RawMaterial::getCurrentStock)
                            .filter(java.util.Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .sum();

                    double reservedStock = getReservedStockForMaterial(rawMaterial, null);
                    double totalAvailable = Math.max(0.0, physicalStock - reservedStock);

                    Double totalCost = calculateFifoCostForMaterial(rawMaterial, requiredQuantity);
                    Double stockDeficit = Math.max(0, requiredQuantity - totalAvailable);
                    Double weightedUnitCost = requiredQuantity > 0 ? (totalCost / requiredQuantity) : (rawMaterial.getUnitCost() != null ? rawMaterial.getUnitCost() : 0.0);
                    String displayName = rawMaterial.getDisplayName();

                    totalRawMaterialCost += totalCost;

                    requirements.add(RawMaterialRequirementDto.builder()
                            .rawMaterialId(materialId)
                            .rawMaterialName(displayName)
                            .materialCode(rawMaterial.getMaterialCode())
                            .requiredQuantity(roundToThreeDecimals(requiredQuantity))
                            .unitOfMeasure(rawMaterial.getUnitOfMeasure())
                            .unitCost(roundToTwoDecimals(weightedUnitCost))
                            .totalCost(roundToTwoDecimals(totalCost))
                            .availableStock(roundToThreeDecimals(totalAvailable))
                            .stockDeficit(roundToThreeDecimals(stockDeficit))
                            .productionCenterName(productionCenterName)
                            .productionCenterId(center != null ? center.getId() : null)
                            .build());
                }
            }
        }

        return com.plover.backerymanagmentsystem.manager.dto.EnhancedProductionPlanResponseDto.builder()
                .planId(null)
                .planName(requestDto.getPlanName())
                .status("DRAFT")
                .rawMaterialRequirements(requirements)
                .totalRawMaterialCost(roundToTwoDecimals(totalRawMaterialCost))
                .build();
    }

    private void calculateRawMaterialRequirementsForProduct(Long productId, Double quantity, 
            ProductionCenter center, Map<ProductionCenter, Map<Long, Double>> centerWiseRequirements, 
            Map<Long, RawMaterial> rawMaterialMap, java.util.Set<Long> visitedProducts) {
        calculateRawMaterialRequirementsForProduct(productId, quantity, center, centerWiseRequirements, rawMaterialMap, visitedProducts, true);
    }

    private void calculateRawMaterialRequirementsForProduct(Long productId, Double quantity, 
            ProductionCenter center, Map<ProductionCenter, Map<Long, Double>> centerWiseRequirements, 
            Map<Long, RawMaterial> rawMaterialMap, java.util.Set<Long> visitedProducts, boolean recurseChildProducts) {
        
        if (quantity == null || quantity <= 0.00001) {
            return;
        }

        if (visitedProducts.contains(productId)) {
            log.warn("Circular BOM dependency detected for product ID: {}. Skipping.", productId);
            return;
        }
        visitedProducts.add(productId);

        log.debug("Processing raw materials for product ID: {} with quantity: {}", productId, quantity);
        List<BillOfMaterial> bomItems = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(productId);
        
        boolean hasBomRawMaterials = false;
        if (bomItems != null && !bomItems.isEmpty()) {
            for (BillOfMaterial bomItem : bomItems) {
                if (bomItem.getChildType() == BillOfMaterial.ChildType.raw_material) {
                    Long materialId = bomItem.getChildItemId();
                    Double requiredQuantity = bomItem.getQuantity().doubleValue() * quantity;
                    
                    RawMaterial rawMaterial = resolveRawMaterial(materialId);
                    if (rawMaterial != null) {
                        Long actualId = rawMaterial.getId();
                        if (!rawMaterialMap.containsKey(actualId)) {
                            rawMaterialMap.put(actualId, rawMaterial);
                        }
                        ProductionCenter targetCenter = center;
                        if (bomItem.getProductionCenterId() != null) {
                            targetCenter = productionCenterRepository.findById(bomItem.getProductionCenterId()).orElse(center);
                        }
                        if (targetCenter == null && productId != null) {
                            Product p = productRepository.findById(productId).orElse(null);
                            if (p != null && p.getProductionCenterId() != null) {
                                targetCenter = productionCenterRepository.findById(p.getProductionCenterId()).orElse(null);
                            }
                        }
                        centerWiseRequirements
                                .computeIfAbsent(targetCenter, k -> new HashMap<>())
                                .merge(actualId, requiredQuantity, Double::sum);
                        hasBomRawMaterials = true;
                    }
                } else if (bomItem.getChildType() == BillOfMaterial.ChildType.product && recurseChildProducts) {
                    Long childProductId = bomItem.getChildItemId();
                    Double childQuantity = bomItem.getQuantity().doubleValue() * quantity;
                    ProductionCenter childCenter = center;
                    if (bomItem.getProductionCenterId() != null) {
                        childCenter = productionCenterRepository.findById(bomItem.getProductionCenterId()).orElse(center);
                    }
                    if (childCenter == null && childProductId != null) {
                        Product p = productRepository.findById(childProductId).orElse(null);
                        if (p != null && p.getProductionCenterId() != null) {
                            childCenter = productionCenterRepository.findById(p.getProductionCenterId()).orElse(null);
                        }
                    }
                    calculateRawMaterialRequirementsForProduct(childProductId, childQuantity,
                            childCenter, centerWiseRequirements, rawMaterialMap, visitedProducts, recurseChildProducts);
                    hasBomRawMaterials = true;
                }
            }
        }

        if (!hasBomRawMaterials) {
            // Recipe fallback: if product has no BOM raw material entries, check if it has RecipeIngredients
            com.plover.backerymanagmentsystem.manager.model.Recipe recipe = recipeRepository
                    .findByProductIdAndIsActiveTrue(productId)
                    .orElse(null);
            if (recipe != null && recipe.getRecipeIngredients() != null) {
                for (com.plover.backerymanagmentsystem.manager.model.RecipeIngredient ingredient : recipe.getRecipeIngredients()) {
                    if (ingredient.getRawMaterial() != null) {
                        RawMaterial rawMaterial = ingredient.getRawMaterial();
                        Long actualId = rawMaterial.getId();
                        Double requiredQuantity = ingredient.getQuantityPerUnit() * quantity;
                        if (!rawMaterialMap.containsKey(actualId)) {
                            rawMaterialMap.put(actualId, rawMaterial);
                        }
                        centerWiseRequirements
                                .computeIfAbsent(center, k -> new HashMap<>())
                                .merge(actualId, requiredQuantity, Double::sum);
                    }
                }
            }
        }
        visitedProducts.remove(productId);
    }


    @Override
    public Double calculateTotalRawMaterialCost(Long planId) {
        List<RawMaterialRequirementDto> requirements = calculateRawMaterialRequirements(planId);
        return requirements.stream()
                .mapToDouble(RawMaterialRequirementDto::getTotalCost)
                .sum();
    }

    @Override
    public void deleteProductionPlan(Long planId) {
        log.info("Starting deletion process for production plan: {}", planId);
        
        ProductionPlan productionPlan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + planId));

        // Check if production plan status is DRAFT or it's a template - only DRAFT plans or templates can be deleted
        if (productionPlan.getStatus() != ProductionPlan.ProductionPlanStatus.DRAFT && !Boolean.TRUE.equals(productionPlan.getIsTemplate())) {
            throw new RuntimeException("Cannot delete production plan with status: " + productionPlan.getStatus() + 
                    ". Only templates or plans with DRAFT status can be deleted.");
        }

        log.info("Production plan '{}' found with DRAFT status or is a template. Proceeding with cascade deletion.", productionPlan.getPlanName());

        // 1. Delete ProductionPlanMaterials (no cascade defined in entity)
        log.info("Deleting production plan materials for plan: {}", planId);
        productionPlanMaterialsRepository.deleteByProductionPlan_Id(planId);

        // 1b. Release semi-finished stock reservations
        log.info("Releasing semi-finished stock reservations for plan: {}", planId);
        semiFinishedReservationService.releaseReservationsForPlan(planId);

        // 2. Delete related DistributionPlans and their items (created during plan creation)
        log.info("Deleting related distribution plans for plan id: {}", planId);
        List<DistributionPlan> relatedDistributionPlans = distributionPlanRepository.findByProductionPlan_Id(planId);
        for (DistributionPlan distributionPlan : relatedDistributionPlans) {
            log.info("Deleting distribution plan: {} (ID: {})", distributionPlan.getName(), distributionPlan.getDpId());
            // DistributionPlanItems will be deleted automatically due to cascade in DistributionPlan entity
            distributionPlanRepository.delete(distributionPlan);
        }

        // 3. Delete ProductionPlan (ProductionPlanItems will be deleted automatically due to cascade)
        log.info("Deleting production plan: {} (ID: {})", productionPlan.getPlanName(), planId);
        productionPlanRepository.delete(productionPlan);
        
        log.info("Successfully deleted production plan: {} and all related entities", planId);
    }

    private ProductionPlanResponseDto buildProductionPlanResponse(ProductionPlan productionPlan,
            List<RawMaterialRequirementDto> rawMaterialRequirements,
            Double totalRawMaterialCost) {
        List<ProductionPlanItemResponseDto> itemResponses = productionPlan.getProductionPlanItems().stream()
                .map(item -> {
                    String type = item.getProductId() != null ? "product" : "raw_material";
                    return ProductionPlanItemResponseDto.builder()
                            .id(item.getId())
                            .productId(item.getProductId())
                            .rawMaterialId(item.getRawMaterialId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .exactQuantity(item.getExactQuantity())
                            .unitOfMeasure(item.getUnitOfMeasure())
                            .unitCost(item.getUnitCost())
                            .totalCost(item.getTotalCost())
                            .estimatedRawMaterialCost(item.getEstimatedRawMaterialCost())
                            .type(type)
                            .isTopLevel(item.getIsTopLevel())
                            .parentPlanItemId(item.getParentPlanItemId())
                            .productionCenterId(item.getProductionCenterId())
                            .reservedFromMiniStore(item.getReservedFromMiniStore())
                            .miniStoreFulfilled(item.getMiniStoreFulfilled())
                            .build();
                })
                .collect(Collectors.toList());

        // Fetch coupled distribution plans
        List<DistributionPlan> distPlans = distributionPlanRepository.findByProductionPlan_Id(productionPlan.getId());
        List<DistributionPlanDto> distPlanDtos = distPlans.stream()
                .map(dp -> DistributionPlanDto.builder()
                        .dpId(dp.getDpId())
                        .name(dp.getName())
                        .outletId(dp.getOutlet() != null ? dp.getOutlet().getOutletId() : null)
                        .outletName(dp.getOutlet() != null ? dp.getOutlet().getName() : null)
                        .date(dp.getDate())
                        .isActive(dp.getIsActive())
                        .status(dp.getStatus() != null ? dp.getStatus().name() : null)
                        .items(dp.getDistributionPlanItems() != null ? dp.getDistributionPlanItems().stream()
                                .map(dpi -> DistributionPlanItemDto.builder()
                                        .dpiId(dpi.getDpiId())
                                        .productId(dpi.getProduct() != null ? dpi.getProduct().getId() : null)
                                        .productName(dpi.getProduct() != null ? dpi.getProduct().getProductName() : null)
                                        .qty(dpi.getQty())
                                        .dpId(dp.getDpId())
                                        .build())
                                .collect(Collectors.toList()) : new ArrayList<>())
                        .build())
                .collect(Collectors.toList());

        return ProductionPlanResponseDto.builder()
                .id(productionPlan.getId())
                .planName(productionPlan.getPlanName())
                .department(productionPlan.getDepartment())
                .planDate(productionPlan.getPlanDate())
                .status(productionPlan.getStatus() != null ? productionPlan.getStatus().name() : null)
                .totalEstimatedCost(productionPlan.getTotalEstimatedCost())
                .notes(productionPlan.getNotes())
                .createdAt(productionPlan.getCreatedAt())
                .updatedAt(productionPlan.getUpdatedAt())
                .productionItems(itemResponses)
                .rawMaterialRequirements(rawMaterialRequirements)
                .totalRawMaterialCost(totalRawMaterialCost)
                .isTemplate(productionPlan.getIsTemplate())
                .distributionPlans(distPlanDtos)
                .build();
    }

    @Override
    public List<ProductionPlanResponseDto> getProductionPlanTemplates() {
        log.info("Fetching all production plan templates / previous plans");
        List<ProductionPlan> templates = productionPlanRepository.findByOrderByCreatedAtDesc();
        return templates.stream()
                .map(plan -> {
                    List<RawMaterialRequirementDto> rawMaterialRequirements = calculateRawMaterialRequirements(plan.getId());
                    Double totalRawMaterialCost = calculateTotalRawMaterialCost(plan.getId());
                    return buildProductionPlanResponse(plan, rawMaterialRequirements, totalRawMaterialCost);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductionPlanResponseDto updateProductionPlan(Long planId, ProductionPlanRequestDto requestDto) {
        log.info("Updating production plan with ID: {}", planId);

        ProductionPlan existingPlan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Production plan not found: " + planId));

        // Update basic fields
        existingPlan.setPlanName(requestDto.getPlanName());
        existingPlan.setPlanDate(requestDto.getPlanDate() != null ? requestDto.getPlanDate().toLocalDateTime() : null);
        existingPlan.setNotes(requestDto.getNotes());
        existingPlan.setDepartment(requestDto.getDepartment());
        existingPlan.setIsTemplate(requestDto.getIsTemplate() != null ? requestDto.getIsTemplate() : false);

        if (requestDto.getStatus() != null) {
            try {
                String requested = requestDto.getStatus().toUpperCase();
                if ("SUBMITTED".equals(requested)) {
                    existingPlan.setStatus(ProductionPlan.ProductionPlanStatus.APPROVED);
                } else {
                    existingPlan.setStatus(ProductionPlan.ProductionPlanStatus.valueOf(requested));
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status '{}' provided. Keeping existing status.", requestDto.getStatus());
            }
        }

        // Update items - for simplicity, we clear and recreate
        // Note: ProductionPlanItem has cascade = CascadeType.ALL, so clearing the list might work if orphanRemoval=true
        // But ProductionPlan entity doesn't have orphanRemoval=true on items.
        // Let's clear them manually in the repository if needed, or update the entity.
        
        // Actually, let's just clear the existing items from the list and add new ones
        existingPlan.getProductionPlanItems().clear();
        
        List<ProductionPlanItem> newItems = new ArrayList<>();
        double totalEstimatedCost = 0.0;

        for (ProductionPlanItemRequestDto itemDto : requestDto.getProductionItems()) {
            ProductionCenter productionCenter = null;
            if (itemDto.getProductionCenterId() != null) {
                Long centerId = itemDto.getProductionCenterId();
                productionCenter = productionCenterRepository.findById(centerId).orElse(null);
                
                if (productionCenter == null) {
                    log.warn("Production center with ID {} not found in update. Falling back to default.", centerId);
                }
            }

            if (productionCenter == null) {
                productionCenter = productionCenterRepository.findActiveAndEstablished().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("No active, established production centers available"));
            }

            ProductionPlanItem planItem;
            if ("product".equals(itemDto.getType())) {
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + itemDto.getProductId()));

                Long resolvedCenterId = product.getProductionCenterId();
                if (resolvedCenterId == null) {
                    resolvedCenterId = productionCenter.getId();
                }

                double dynamicUnitCost = calculateFifoProductUnitCost(product.getId(), itemDto.getQuantity());

                planItem = ProductionPlanItem.builder()
                        .productionPlan(existingPlan)
                        .productId(itemDto.getProductId())
                        .rawMaterialId(null)
                        .productName(itemDto.getProductName())
                        .quantity(itemDto.getQuantity())
                        .unitCost(dynamicUnitCost)
                        .totalCost(dynamicUnitCost * itemDto.getQuantity())
                        .productionCenterId(resolvedCenterId)
                        .isTopLevel(true)
                        .build();
            } else {
                Long rawMaterialId = itemDto.getRawMaterialId() != null ? itemDto.getRawMaterialId() : itemDto.getProductId();
                RawMaterial rawMaterial = rawMaterialRepository.findById(rawMaterialId)
                        .orElseThrow(() -> new RuntimeException("Raw material not found: " + rawMaterialId));

                planItem = ProductionPlanItem.builder()
                        .productionPlan(existingPlan)
                        .productId(null)
                        .rawMaterialId(rawMaterialId)
                        .productName(itemDto.getProductName())
                        .quantity(itemDto.getQuantity())
                        .unitCost(rawMaterial.getUnitCost())
                        .totalCost(rawMaterial.getUnitCost() * itemDto.getQuantity())
                        .productionCenterId(productionCenter.getId())
                        .isTopLevel(true)
                        .build();
            }
            newItems.add(planItem);
            totalEstimatedCost += planItem.getTotalCost();
        }
        
        existingPlan.getProductionPlanItems().clear();
        existingPlan.getProductionPlanItems().addAll(newItems);
        existingPlan.setTotalEstimatedCost(totalEstimatedCost);

        // Save updated plan
        ProductionPlan savedPlan = productionPlanRepository.save(existingPlan);

        List<ProductionPlanItem> childItems = new ArrayList<>();
        java.util.Set<Long> visited = new java.util.HashSet<>();
        for (ProductionPlanItem planItem : savedPlan.getProductionPlanItems()) {
            if (planItem.getProductId() != null && Boolean.TRUE.equals(planItem.getIsTopLevel())) {
                expandChildProducts(planItem, planItem.getQuantity().doubleValue(), 
                        planItem.getProductionCenterId(), childItems, savedPlan, visited);
            }
        }
        if (!childItems.isEmpty()) {
            savedPlan.getProductionPlanItems().addAll(childItems);
            savedPlan = productionPlanRepository.save(savedPlan);
        }

        // Re-calculate and save raw material requirements
        List<RawMaterialRequirementDto> rawMaterialRequirements = calculateRawMaterialRequirements(savedPlan.getId());
        Double totalRawMaterialCost = calculateTotalRawMaterialCost(savedPlan.getId());
        saveProductionPlanMaterials(savedPlan.getId(), rawMaterialRequirements);

        // Update total cost (do not double count raw materials by adding totalRawMaterialCost)
        savedPlan.setTotalEstimatedCost(totalEstimatedCost);
        productionPlanRepository.save(savedPlan);

        // Generate IngredientRequests if plan is APPROVED/SUBMITTED
        generateIngredientRequestsIfApproved(savedPlan, rawMaterialRequirements);

        // Sync distribution plans
        // 1. Delete old distribution plans
        List<DistributionPlan> oldDistPlans = distributionPlanRepository.findByProductionPlan_Id(savedPlan.getId());
        distributionPlanRepository.deleteAll(oldDistPlans);

        // 2. Create new distribution plans from allocations
        createDistributionPlansFromAllocations(requestDto, savedPlan);

        return buildProductionPlanResponse(savedPlan, rawMaterialRequirements, totalRawMaterialCost);
    }

    private void expandChildProducts(ProductionPlanItem parentItem, double parentQty, Long defaultCenterId, 
            List<ProductionPlanItem> targetList, ProductionPlan plan, java.util.Set<Long> visited) {
        if (parentItem == null || parentItem.getProductId() == null || visited.contains(parentItem.getProductId())) {
            return;
        }
        visited.add(parentItem.getProductId());

        List<BillOfMaterial> bomItems = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(parentItem.getProductId());
        for (BillOfMaterial bomItem : bomItems) {
            if (bomItem.getChildType() == BillOfMaterial.ChildType.product) {
                Long childProductId = bomItem.getChildItemId();
                Product childProduct = productRepository.findById(childProductId).orElse(null);
                if (childProduct != null) {
                    double childQty = bomItem.getQuantity().doubleValue() * parentQty;
                    
                    Long resolvedCenterId = childProduct.getProductionCenterId();
                    if (resolvedCenterId == null) {
                        resolvedCenterId = defaultCenterId;
                    }

                    Long miniStoreId = null;
                    if (resolvedCenterId != null) {
                        ProductionCenter center = productionCenterRepository.findById(resolvedCenterId).orElse(null);
                        if (center != null && center.getMiniStore() != null) {
                            miniStoreId = center.getMiniStore().getMiniStoreId().longValue();
                        }
                    }

                    double dynamicUnitCost = calculateFifoProductUnitCost(childProduct.getId(), childQty > 0 ? childQty : 1.0);
                    String childUom = childProduct.getUnitOfMeasure() != null ? childProduct.getUnitOfMeasure() : "Kg";

                    ProductionPlanItem childItem = ProductionPlanItem.builder()
                            .productionPlan(plan)
                            .productId(childProductId)
                            .rawMaterialId(null)
                            .productName(childProduct.getProductName())
                            .quantity((int) Math.ceil(childQty))
                            .exactQuantity(childQty)
                            .unitOfMeasure(childUom)
                            .unitCost(dynamicUnitCost)
                            .estimatedRawMaterialCost(dynamicUnitCost * childQty)
                            .totalCost(dynamicUnitCost * childQty)
                            .productionCenterId(resolvedCenterId)
                            .isTopLevel(false)
                            .parentPlanItemId(parentItem.getId())
                            .reservedFromMiniStore(0.0)
                            .miniStoreFulfilled(false)
                            .build();
                    ProductionPlanItem savedChildItem = productionPlanItemRepository.save(childItem);

                    // FIFO Reservation from Mini Store batch inventory
                    ReservationResult reservationResult = semiFinishedReservationService.reserveSemiFinishedStock(
                            plan.getId(), savedChildItem.getId(), childProductId, miniStoreId, childQty);

                    double netChildQtyNeeded = reservationResult.getNetNeededQty();
                    int finalQty = (int) Math.ceil(netChildQtyNeeded);

                    savedChildItem.setQuantity(finalQty);
                    savedChildItem.setExactQuantity(netChildQtyNeeded);
                    savedChildItem.setReservedFromMiniStore(reservationResult.getReservedQty());
                    savedChildItem.setMiniStoreFulfilled(reservationResult.getFullyFulfilled());
                    savedChildItem.setTotalCost(dynamicUnitCost * netChildQtyNeeded);
                    savedChildItem.setEstimatedRawMaterialCost(dynamicUnitCost * netChildQtyNeeded);
                    savedChildItem = productionPlanItemRepository.save(savedChildItem);

                    targetList.add(savedChildItem);

                    // Recurse for child product components using the exact required quantity
                    double qtyToExpand = netChildQtyNeeded > 0 ? netChildQtyNeeded : childQty;
                    expandChildProducts(savedChildItem, qtyToExpand, resolvedCenterId, targetList, plan, visited);
                }
            }
        }
        visited.remove(parentItem.getProductId());
    }

    private double calculateProductUnitCost(Long productId) {
        return calculateProductUnitCostRecursive(productId, new java.util.HashSet<>());
    }

    private double calculateProductUnitCostRecursive(Long productId, java.util.Set<Long> visited) {
        if (visited.contains(productId)) {
            log.warn("Circular BOM dependency detected for product ID {} during unit cost calculation", productId);
            return 0.0;
        }
        visited.add(productId);
        
        List<BillOfMaterial> bomItems = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(productId);
        if (bomItems.isEmpty()) {
            com.plover.backerymanagmentsystem.manager.model.Recipe recipe = recipeRepository
                    .findByProductIdAndIsActiveTrue(productId)
                    .orElse(null);
            if (recipe != null && recipe.getRecipeIngredients() != null && !recipe.getRecipeIngredients().isEmpty()) {
                double recipeCost = 0.0;
                for (com.plover.backerymanagmentsystem.manager.model.RecipeIngredient ingredient : recipe.getRecipeIngredients()) {
                    if (ingredient.getRawMaterial() != null && ingredient.getQuantityPerUnit() != null) {
                        double rmCost = ingredient.getRawMaterial().getUnitCost() != null ? ingredient.getRawMaterial().getUnitCost() : 0.0;
                        recipeCost += rmCost * ingredient.getQuantityPerUnit();
                    }
                }
                visited.remove(productId);
                if (recipeCost > 0) return recipeCost;
            }
            Product product = productRepository.findById(productId).orElse(null);
            visited.remove(productId);
            return product != null && product.getUnitPrice() != null ? product.getUnitPrice() : 0.0;
        }

        double totalCost = 0.0;
        for (BillOfMaterial item : bomItems) {
            double childCost = 0.0;
            if (item.getChildType() == BillOfMaterial.ChildType.raw_material) {
                RawMaterial rm = resolveRawMaterial(item.getChildItemId());
                if (rm != null) {
                    childCost = rm.getUnitCost() != null ? rm.getUnitCost() : 0.0;
                }
            } else if (item.getChildType() == BillOfMaterial.ChildType.product) {
                childCost = calculateProductUnitCostRecursive(item.getChildItemId(), visited);
            }
            totalCost += childCost * item.getQuantity().doubleValue();
        }
        visited.remove(productId);
        return totalCost;
    }

    private double calculateFifoProductUnitCost(Long productId, double productQuantity) {
        if (productQuantity <= 0) return 0.0;
        Map<Long, Double> requirements = new HashMap<>();
        Map<Long, RawMaterial> rawMaterialMap = new HashMap<>();
        gatherRequirementsFromBom(productId, productQuantity, requirements, rawMaterialMap, new java.util.HashSet<>());
        double totalCost = 0.0;
        for (Map.Entry<Long, Double> entry : requirements.entrySet()) {
            Long materialId = entry.getKey();
            Double requiredQty = entry.getValue();
            RawMaterial rm = rawMaterialMap.get(materialId);
            if (rm != null) {
                double materialCost = calculateFifoCostForMaterial(rm, requiredQty);
                if (materialCost <= 0 && rm.getUnitCost() != null && rm.getUnitCost() > 0) {
                    materialCost = rm.getUnitCost() * requiredQty;
                }
                totalCost += materialCost;
            }
        }
        if (totalCost <= 0) {
            double recursiveCost = calculateProductUnitCost(productId);
            return recursiveCost;
        }
        return totalCost / productQuantity;
    }

    private void gatherRequirementsFromBom(Long productId, Double quantity, 
            Map<Long, Double> requirements, Map<Long, RawMaterial> rawMaterialMap, java.util.Set<Long> visited) {
        if (visited.contains(productId)) return;
        visited.add(productId);
        List<BillOfMaterial> bomItems = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(productId);
        boolean hasBomItems = false;
        if (bomItems != null && !bomItems.isEmpty()) {
            for (BillOfMaterial bomItem : bomItems) {
                if (bomItem.getChildType() == BillOfMaterial.ChildType.raw_material) {
                    RawMaterial rm = resolveRawMaterial(bomItem.getChildItemId());
                    if (rm != null) {
                        rawMaterialMap.put(rm.getId(), rm);
                        requirements.merge(rm.getId(), bomItem.getQuantity().doubleValue() * quantity, Double::sum);
                        hasBomItems = true;
                    }
                } else if (bomItem.getChildType() == BillOfMaterial.ChildType.product) {
                    gatherRequirementsFromBom(bomItem.getChildItemId(), bomItem.getQuantity().doubleValue() * quantity,
                            requirements, rawMaterialMap, visited);
                    hasBomItems = true;
                }
            }
        }
        if (!hasBomItems) {
            // Recipe fallback: if product has no BOM raw material entries, check if it has RecipeIngredients
            com.plover.backerymanagmentsystem.manager.model.Recipe recipe = recipeRepository
                    .findByProductIdAndIsActiveTrue(productId)
                    .orElse(null);
            if (recipe != null && recipe.getRecipeIngredients() != null) {
                for (com.plover.backerymanagmentsystem.manager.model.RecipeIngredient ingredient : recipe.getRecipeIngredients()) {
                    if (ingredient.getRawMaterial() != null) {
                        RawMaterial rawMaterial = ingredient.getRawMaterial();
                        Long actualId = rawMaterial.getId();
                        Double requiredQuantity = ingredient.getQuantityPerUnit() * quantity;
                        rawMaterialMap.put(actualId, rawMaterial);
                        requirements.merge(actualId, requiredQuantity, Double::sum);
                    }
                }
            }
        }
        visited.remove(productId);
    }

    private RawMaterial resolveRawMaterial(Long id) {
        // 1. Try finding in raw_materials table directly
        RawMaterial rm = rawMaterialRepository.findById(id).orElse(null);
        if (rm != null) return rm;

        // 2. Fallback: Check if it's a recipe_ingredient ID (cross-referenced by name/code)
        // This handles cases where BOM refers to recipe_ingredients entries
        log.debug("Raw material not found by ID {}. Attempting fallback via recipe repository.", id);
        RawMaterial riFallback = recipeIngredientRepository.findById(id)
                .map(ingredient -> {
                    String name = ingredient.getRawMaterial() != null ? ingredient.getRawMaterial().getMaterialName() : null;
                    if (name == null) return null;
                    return rawMaterialRepository.findAll().stream()
                            .filter(m -> name.equalsIgnoreCase(m.getMaterialName()))
                            .findFirst()
                            .orElse(null);
                })
                .orElse(null);
        if (riFallback != null) return riFallback;
        
        // 3. Fallback: Check if it is a Product incorrectly mapped as raw_material in BOM
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            RawMaterial dummyRm = new RawMaterial();
            dummyRm.setId(product.getId());
            dummyRm.setMaterialName(product.getProductName());
            dummyRm.setMaterialCode(product.getProductCode());
            dummyRm.setUnitOfMeasure(product.getUnitOfMeasure() != null ? product.getUnitOfMeasure() : "unit");
            dummyRm.setUnitCost(product.getUnitPrice() != null ? product.getUnitPrice() : 0.0);
            return dummyRm;
        }

        return null;
    }

    private Double calculateFifoCostForMaterial(RawMaterial defaultMaterial, Double requiredQuantity) {
        if (requiredQuantity == null || requiredQuantity <= 0) {
            return 0.0;
        }

        String materialCode = defaultMaterial.getMaterialCode();
        String genericName = defaultMaterial.getGenericMaterialName();
        List<RawMaterial> batches;
        if (genericName != null && !genericName.trim().isEmpty()) {
            batches = rawMaterialRepository.findAllByGenericMaterialName(genericName);
        } else if (materialCode != null) {
            batches = rawMaterialRepository.findAllByMaterialCode(materialCode);
        } else {
            batches = List.of(defaultMaterial);
        }

        if (batches == null || batches.isEmpty()) {
            return requiredQuantity * (defaultMaterial.getUnitCost() != null ? defaultMaterial.getUnitCost() : 0.0);
        }

        // Sort by ID ascending (First Come First Serve / FIFO)
        List<RawMaterial> sortedBatches = batches.stream()
                .filter(b -> b.getCurrentStock() != null && b.getCurrentStock() > 0)
                .sorted(java.util.Comparator.comparing(RawMaterial::getId))
                .collect(Collectors.toList());

        double remaining = requiredQuantity;
        double totalCost = 0.0;

        for (RawMaterial batch : sortedBatches) {
            if (remaining <= 0) {
                break;
            }
            double stock = batch.getCurrentStock();
            double consumed = Math.min(remaining, stock);
            double unitCost = batch.getUnitCost() != null ? batch.getUnitCost() : 0.0;
            totalCost += consumed * unitCost;
            remaining -= consumed;
        }

        // If there's still a deficit, price it using the default material's unit cost
        if (remaining > 0) {
            double defaultUnitCost = defaultMaterial.getUnitCost() != null ? defaultMaterial.getUnitCost() : 0.0;
            totalCost += remaining * defaultUnitCost;
        }

        return totalCost;
    }

    private Double roundToThreeDecimals(Double value) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Double roundToTwoDecimals(Double value) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void generateIngredientRequestsIfApproved(ProductionPlan savedPlan, List<RawMaterialRequirementDto> requirements) {
        if (savedPlan.getStatus() != ProductionPlan.ProductionPlanStatus.APPROVED && 
            savedPlan.getStatus() != ProductionPlan.ProductionPlanStatus.SUBMITTED) {
            log.info("DEBUG: Plan status is {}, not generating", savedPlan.getStatus());
            return;
        }

        // Check existing ingredient requests for this plan by center
        List<IngredientRequest> existingRequests = ingredientRequestRepository.findByProductionPlanId(savedPlan.getId());
        Map<Long, IngredientRequest> existingByCenter = existingRequests.stream()
                .filter(r -> r.getProductionCenterId() != null)
                .collect(Collectors.toMap(IngredientRequest::getProductionCenterId, r -> r, (r1, r2) -> r1));

        log.info("Automatically generating ingredient requests for production plan ID: {}", savedPlan.getId());
        log.info("DEBUG: Total requirements: {}", requirements.size());

        // Group requirements by production center (fallback to default center if requirement has null productionCenterId)
        Long defaultCenterId = productionCenterRepository.findActiveAndEstablished().stream()
                .map(ProductionCenter::getId).findFirst().orElse(1L);

        Map<Long, List<RawMaterialRequirementDto>> requirementsByCenter = requirements.stream()
                .filter(req -> {
                    boolean valid = req.getRequiredQuantity() != null && req.getRequiredQuantity() > 0;
                    log.info("DEBUG: Requirement material={}, centerId={}, reqQty={}, isValid={}", 
                        req.getRawMaterialName(), req.getProductionCenterId(), req.getRequiredQuantity(), valid);
                    return valid;
                })
                .collect(Collectors.groupingBy(req -> req.getProductionCenterId() != null ? req.getProductionCenterId() : defaultCenterId));

        log.info("DEBUG: requirementsByCenter keyset: {}", requirementsByCenter.keySet());

        for (Map.Entry<Long, List<RawMaterialRequirementDto>> entry : requirementsByCenter.entrySet()) {
            Long centerId = entry.getKey();
            List<RawMaterialRequirementDto> centerReqs = entry.getValue();

            if (existingByCenter.containsKey(centerId)) {
                log.info("Ingredient request already exists for plan ID: {} and centerId: {}", savedPlan.getId(), centerId);
                continue;
            }
            log.info("DEBUG: Creating request for centerId {} with {} items", centerId, centerReqs.size());

            IngredientRequest request = IngredientRequest.builder()
                    .productionPlanId(savedPlan.getId())
                    .productionCenterId(centerId)
                    .status(IngredientRequestStatus.PENDING)
                    .notes("Auto-generated for Production Plan: " + savedPlan.getPlanName())
                    .build();

            List<IngredientRequestItem> items = centerReqs.stream().map(req -> 
                    IngredientRequestItem.builder()
                            .request(request)
                            .rawMaterialId(req.getRawMaterialId())
                            .rawMaterialName(req.getRawMaterialName())
                            .requestedQty(req.getRequiredQuantity())
                            .unitOfMeasure(req.getUnitOfMeasure())
                            .build()
            ).collect(Collectors.toList());

            request.setItems(items);
            try {
                ingredientRequestRepository.save(request);
                log.info("DEBUG: Successfully saved request for centerId {}", centerId);
            } catch (Exception e) {
                log.error("DEBUG: Failed to save request for centerId {}", centerId, e);
            }
        }
    }
}
