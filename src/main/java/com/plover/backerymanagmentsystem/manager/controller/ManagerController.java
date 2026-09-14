package com.plover.backerymanagmentsystem.manager.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.manager.dto.DistributionPlanDto;
import com.plover.backerymanagmentsystem.manager.dto.DistributionPlanItemDto;
import com.plover.backerymanagmentsystem.manager.dto.DistributionPlanRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.MiniStoreDto;
import com.plover.backerymanagmentsystem.manager.dto.MiniStoreItemDto;
import com.plover.backerymanagmentsystem.manager.dto.MiniStoreUpdateRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.OutletDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductAndRawMaterialDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanDetailResponseDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanResponseDto;
import com.plover.backerymanagmentsystem.manager.dto.RawMaterialRequirementDto;
import com.plover.backerymanagmentsystem.manager.dto.EnhancedProductionPlanResponseDto;
import com.plover.backerymanagmentsystem.manager.dto.RawMaterialSummaryDto;
import com.plover.backerymanagmentsystem.manager.dto.StockAdjustmentApprovalRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.StockAdjustmentApprovalResponseDto;
import com.plover.backerymanagmentsystem.manager.dto.UpdateReturnItemStatusRequestDto;
import com.plover.backerymanagmentsystem.manager.model.DistributionPlan;
import com.plover.backerymanagmentsystem.manager.model.DistributionPlanItem;
import com.plover.backerymanagmentsystem.manager.model.DistributionPlanStatus;
import com.plover.backerymanagmentsystem.manager.model.MiniStore;
import com.plover.backerymanagmentsystem.manager.model.MiniStoreItem;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.DistributionPlanItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.DistributionPlanRepository;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionPlanMaterialsRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ManagerPurchaseOrderRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.service.MiniStoreService;
import com.plover.backerymanagmentsystem.manager.service.OutletService;
import com.plover.backerymanagmentsystem.manager.service.ProductionPlanningService;
import com.plover.backerymanagmentsystem.manager.service.RawMaterialService;
import com.plover.backerymanagmentsystem.manager.service.StockAdjustmentApprovalService;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem.ReturnStatus;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialReturnItemRepository;

import com.plover.backerymanagmentsystem.pos.service.DayProductionService;
import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ManagerController {

    private final ProductionPlanningService productionPlanningService;
    private final ProductRepository productRepository;
    private final ProductionPlanItemRepository productionPlanItemRepository;
    private final ProductionPlanMaterialsRepository productionPlanMaterialsRepository;
    private final MiniStoreService miniStoreService;
    private final OutletRepository outletRepository;
    private final DistributionPlanRepository distributionPlanRepository;
    private final DistributionPlanItemRepository distributionPlanItemRepository;
    private final MiniStoreRepository miniStoreRepository;
    private final MiniStoreItemRepository miniStoreItemRepository;
    private final StockAdjustmentApprovalService stockAdjustmentApprovalService;
    private final RawMaterialReturnItemRepository rawMaterialReturnItemRepository;
    private final RawMaterialService rawMaterialService;
    private final OutletService outletService;
    private final ProductionCenterRepository productionCenterRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final com.plover.backerymanagmentsystem.store_keeper.service.PurchaseOrderCreationService purchaseOrderCreationService;
    private final DayProductionService dayProductionService;

    @PostMapping("/production-plan")
    public ResponseEntity<EnhancedProductionPlanResponseDto> createProductionPlan(
            @Valid @RequestBody ProductionPlanRequestDto requestDto) {
        
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.info("Production Plan Request from user: {}, Authorities: {}", auth.getName(), auth.getAuthorities());
        } else {
            log.warn("Production Plan Request from UNAUTHENTICATED user");
        }

        log.info("Received request to create production plan: {}", requestDto.getPlanName());
        try {
            EnhancedProductionPlanResponseDto response = productionPlanningService.createProductionPlanWithBom(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating production plan: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/production-plan/preview-materials")
    public ResponseEntity<EnhancedProductionPlanResponseDto> previewProductionPlanMaterials(
            @Valid @RequestBody ProductionPlanRequestDto requestDto) {
        log.info("Received request to preview raw material requirements for production plan: {}", requestDto.getPlanName());
        try {
            EnhancedProductionPlanResponseDto response = productionPlanningService.previewProductionPlanMaterials(requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error previewing production plan materials: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping({"/production-plan/{planId}", "/production-plans/{planId}"})
    public ResponseEntity<ProductionPlanResponseDto> getProductionPlan(@PathVariable Long planId) {
        log.info("Fetching production plan with ID: {}", planId);

        try {
            ProductionPlanResponseDto response = productionPlanningService.getProductionPlanById(planId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching production plan: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/production-plans")
    public ResponseEntity<List<ProductionPlanResponseDto>> getAllProductionPlans() {
        log.info("Fetching all production plans");

        try {
            List<ProductionPlanResponseDto> response = productionPlanningService.getAllProductionPlans();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching production plans: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/production-plan/{planId}/status")
    public ResponseEntity<ProductionPlanResponseDto> updateProductionPlanStatus(
            @PathVariable Long planId,
            @RequestParam String status) {
        log.info("Updating production plan {} status to: {}", planId, status);

        try {
            ProductionPlanResponseDto response = productionPlanningService.updateProductionPlanStatus(planId, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating production plan status: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/production-plan/templates")
    public ResponseEntity<List<ProductionPlanResponseDto>> getProductionPlanTemplates() {
        log.info("Fetching all production plan templates");

        try {
            List<ProductionPlanResponseDto> response = productionPlanningService.getProductionPlanTemplates();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching production plan templates: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/production-plan/{planId}")
    public ResponseEntity<ProductionPlanResponseDto> updateProductionPlan(
            @PathVariable Long planId,
            @Valid @RequestBody ProductionPlanRequestDto requestDto) {
        log.info("Received request to update production plan: {}", planId);

        try {
            ProductionPlanResponseDto response = productionPlanningService.updateProductionPlan(planId, requestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating production plan {}: {}", planId, e.getMessage());
            throw e;
        }
    }

    @GetMapping("/production-plan/{planId}/raw-materials")
    public ResponseEntity<List<RawMaterialRequirementDto>> getRawMaterialRequirements(@PathVariable Long planId) {
        log.info("Calculating raw material requirements for plan: {}", planId);

        try {
            List<RawMaterialRequirementDto> response = productionPlanningService.calculateRawMaterialRequirements(planId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating raw material requirements: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/production-plan/{planId}/raw-material-cost")
    public ResponseEntity<Double> getTotalRawMaterialCost(@PathVariable Long planId) {
        log.info("Calculating total raw material cost for plan: {}", planId);

        try {
            Double totalCost = productionPlanningService.calculateTotalRawMaterialCost(planId);
            return ResponseEntity.ok(totalCost);
        } catch (Exception e) {
            log.error("Error calculating total raw material cost: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/raw-materials")
    public ResponseEntity<List<RawMaterialSummaryDto>> getAllRawMaterials() {
        log.info("Fetching all active raw materials for manager dashboard");

        try {
            List<RawMaterialSummaryDto> rawMaterials = rawMaterialService.getAllRawMaterials();
            return ResponseEntity.ok(rawMaterials);
        } catch (Exception e) {
            log.error("Error fetching raw materials: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/outlets")
    public ResponseEntity<List<OutletDto>> getAllManagerOutlets() {
        log.info("Fetching all outlets for manager dashboard");

        try {
            List<OutletDto> outlets = outletService.getAllOutlets();
            return ResponseEntity.ok(outlets);
        } catch (Exception e) {
            log.error("Error fetching outlets: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/production-plan/{planId}")
    public ResponseEntity<Map<String, Object>> deleteProductionPlan(@PathVariable Long planId) {
        log.info("Received request to delete production plan: {}", planId);

        try {
            // Validate planId
            if (planId == null || planId <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid production plan ID. ID must be a positive number.");
                errorResponse.put("planId", planId);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            productionPlanningService.deleteProductionPlan(planId);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Production plan and all related data deleted successfully");
            successResponse.put("planId", planId);
            successResponse.put("deletedAt", java.time.LocalDateTime.now());

            log.info("Successfully deleted production plan: {}", planId);
            return ResponseEntity.ok(successResponse);
        } catch (RuntimeException e) {
            log.error("Error deleting production plan {}: {}", planId, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("planId", planId);

            // Return appropriate HTTP status based on error type
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Cannot delete")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Unexpected error deleting production plan {}: {}", planId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred while deleting the production plan");
            errorResponse.put("planId", planId);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/production-plan/{planId}/materials/kitchen")
    public ResponseEntity<ProductionPlanDetailResponseDto> getPlanMaterialsForKitchen(@PathVariable Long planId) {
        try {
            ProductionPlanResponseDto plan = productionPlanningService.getProductionPlanById(planId);

            var rawMaterials = productionPlanMaterialsRepository.findByProductionPlan_IdAndRawMaterialQuantityForKitchenNotNull(planId)
                    .stream()
                    .map(row -> ProductionPlanDetailResponseDto.RawMaterialDetailDto.builder()
                    .name(row.getRawMaterial().getMaterialName())
                    .quantity(row.getRawMaterialQuantityForKitchen())
                    .unit(row.getRawMaterial().getUnitOfMeasure())
                    .build())
                    .toList();

            var products = plan.getProductionItems().stream()
                    .map(item -> ProductionPlanDetailResponseDto.ProductDetailDto.builder()
                    .name(item.getProductName())
                    .quantity(item.getQuantity())
                    .unit("pcs") // Default unit, you can modify based on your product model
                    .build())
                    .toList();

            var response = ProductionPlanDetailResponseDto.builder()
                    .id("KP-" + planId) // Kitchen Plan prefix
                    .requestName(plan.getPlanName())
                    .requestDate(plan.getPlanDate().toLocalDate().toString())
                    .status(plan.getStatus())
                    .products(products)
                    .rawMaterials(rawMaterials)
                    .createdBy("Manager") // You can get this from user context
                    .createdAt(plan.getCreatedAt().toString())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching kitchen materials for plan {}: {}", planId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/production-plan/{planId}/materials/bakery")
    public ResponseEntity<ProductionPlanDetailResponseDto> getPlanMaterialsForBakery(@PathVariable Long planId) {
        try {
            ProductionPlanResponseDto plan = productionPlanningService.getProductionPlanById(planId);

            var rawMaterials = productionPlanMaterialsRepository.findByProductionPlan_IdAndRawMaterialQuantityForBakeryNotNull(planId)
                    .stream()
                    .map(row -> ProductionPlanDetailResponseDto.RawMaterialDetailDto.builder()
                    .name(row.getRawMaterial().getMaterialName())
                    .quantity(row.getRawMaterialQuantityForBakery())
                    .unit(row.getRawMaterial().getUnitOfMeasure())
                    .build())
                    .toList();

            var products = plan.getProductionItems().stream()
                    .map(item -> ProductionPlanDetailResponseDto.ProductDetailDto.builder()
                    .name(item.getProductName())
                    .quantity(item.getQuantity())
                    .unit("pcs") // Default unit, you can modify based on your product model
                    .build())
                    .toList();

            var response = ProductionPlanDetailResponseDto.builder()
                    .id("BP-" + planId) // Bakery Plan prefix
                    .requestName(plan.getPlanName())
                    .requestDate(plan.getPlanDate().toLocalDate().toString())
                    .status(plan.getStatus())
                    .products(products)
                    .rawMaterials(rawMaterials)
                    .createdBy("Manager") // You can get this from user context
                    .createdAt(plan.getCreatedAt().toString())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching bakery materials for plan {}: {}", planId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductAndRawMaterialDto>> getAllProducts() {
        log.info("Fetching all products and raw materials with production centers");
        List<Product> products = productRepository.findAll();
        List<com.plover.backerymanagmentsystem.manager.model.RawMaterial> rawMaterials = rawMaterialRepository.findAll();
        List<MiniStore> miniStores = miniStoreRepository.findAll();
        List<Outlet> outlets = outletRepository.findAll();
        
        // Pre-fetch all production centers to avoid N+1 queries
        Map<Long, com.plover.backerymanagmentsystem.manager.model.ProductionCenter> centerMap = productionCenterRepository.findActiveAndEstablished()
                .stream().collect(Collectors.toMap(c -> c.getId(), c -> c));

        // Pre-fetch all mini store items and group them by product/raw material ID
        List<MiniStoreItem> allStoreItems = miniStoreItemRepository.findAll();
        Map<Long, List<MiniStoreItem>> itemsByProduct = allStoreItems.stream()
                .filter(i -> i.getProductId() != null)
                .collect(Collectors.groupingBy(MiniStoreItem::getProductId));
        Map<Long, List<MiniStoreItem>> itemsByRawMaterial = allStoreItems.stream()
                .filter(i -> i.getRawMaterialId() != null)
                .collect(Collectors.groupingBy(MiniStoreItem::getRawMaterialId));

        // Query today's production items for each outlet to get outlet stock availability
        Map<Long, List<ProductAndRawMaterialDto.OutletAvailabilityDto>> productOutletAvailability = new HashMap<>();
        for (Outlet outlet : outlets) {
            try {
                List<DayProductionItemResponseDto> items = dayProductionService.getTodayProductionItems(outlet.getOutletId());
                for (DayProductionItemResponseDto item : items) {
                    if (item.getProductId() != null) {
                        int qty = item.getCurrentQty() != null ? item.getCurrentQty() : 0;
                        productOutletAvailability.computeIfAbsent(item.getProductId(), k -> new ArrayList<>())
                                .add(ProductAndRawMaterialDto.OutletAvailabilityDto.builder()
                                        .outletId(outlet.getOutletId())
                                        .outletName(outlet.getName())
                                        .availableQty(qty)
                                        .build());
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch today's production items for outlet {}: {}", outlet.getOutletId(), ex.getMessage());
            }
        }

        List<ProductAndRawMaterialDto> result = new ArrayList<>();
        
        // Add products
        List<ProductAndRawMaterialDto> productDtos = products.stream().map(product -> {
            List<ProductAndRawMaterialDto.ProductionCenterDto> centerDtos = new ArrayList<>();
            if (product.getProductionCenterId() != null && centerMap.containsKey(product.getProductionCenterId())) {
                var center = centerMap.get(product.getProductionCenterId());
                centerDtos.add(ProductAndRawMaterialDto.ProductionCenterDto.builder()
                        .id(center.getId())
                        .centerName(center.getCenterName())
                        .build());
            }

            List<ProductAndRawMaterialDto.MiniStoreAvailabilityDto> availabilityDtos = miniStores.stream()
                .map(store -> {
                    List<MiniStoreItem> items = itemsByProduct.getOrDefault(product.getId(), new ArrayList<>())
                            .stream().filter(i -> i.getMiniStore() != null && i.getMiniStore().getMiniStoreId().equals(store.getMiniStoreId()))
                            .toList();
                    java.math.BigDecimal qty = items.stream()
                            .map(MiniStoreItem::getPhysicalQty)
                            .filter(java.util.Objects::nonNull)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    return ProductAndRawMaterialDto.MiniStoreAvailabilityDto.builder()
                            .miniStoreId(store.getMiniStoreId())
                            .miniStoreName(store.getName())
                            .availableQty(qty)
                            .build();
                })
                .filter(a -> a.getAvailableQty() != null && a.getAvailableQty().compareTo(java.math.BigDecimal.ZERO) > 0)
                .toList();

            List<ProductAndRawMaterialDto.OutletAvailabilityDto> outletAvailabilityDtos = productOutletAvailability.getOrDefault(product.getId(), new ArrayList<>());

            String itemType = "product";
            if (product.getCategory() != null && "Oilman".equalsIgnoreCase(product.getCategory())) {
                itemType = "Oilman";
            } else if (product.getProductionStageRef() != null && 
                       product.getProductionStageRef().getProductionStage() != null && 
                       product.getProductionStageRef().getProductionStage().toLowerCase().contains("semi")) {
                itemType = "Semi-Finished";
            }

            return ProductAndRawMaterialDto.builder()
                    .id(product.getId())
                    .name(product.getProductName())
                    .code(product.getProductCode())
                    .description(product.getDescription())
                    .unitPrice(product.getUnitPrice())
                    .salePrice(product.getSalePrice() != null && product.getSalePrice() > 0 ? product.getSalePrice() : product.getUnitPrice())
                    .category(product.getCategory())
                    .isActive(product.getIsActive())
                    .isFastMoving(product.getIsFastMoving())
                    .type(itemType)
                    .productionCenters(centerDtos)
                    .miniStoreAvailability(availabilityDtos)
                    .outletAvailability(outletAvailabilityDtos)
                    .build();
        })
        .toList();
        
        // Add raw materials
        List<ProductAndRawMaterialDto> rawMaterialDtos = rawMaterials.stream().map(rawMaterial -> {
            List<ProductAndRawMaterialDto.ProductionCenterDto> centerDtos = new ArrayList<>();

            List<ProductAndRawMaterialDto.MiniStoreAvailabilityDto> availabilityDtos = miniStores.stream()
                .map(store -> {
                    List<MiniStoreItem> items = itemsByRawMaterial.getOrDefault(rawMaterial.getId(), new ArrayList<>())
                            .stream().filter(i -> i.getMiniStore() != null && i.getMiniStore().getMiniStoreId().equals(store.getMiniStoreId()))
                            .toList();
                    java.math.BigDecimal qty = items.stream()
                            .map(MiniStoreItem::getPhysicalQty)
                            .filter(java.util.Objects::nonNull)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    return ProductAndRawMaterialDto.MiniStoreAvailabilityDto.builder()
                            .miniStoreId(store.getMiniStoreId())
                            .miniStoreName(store.getName())
                            .availableQty(qty)
                            .build();
                })
                .filter(a -> a.getAvailableQty() != null && a.getAvailableQty().compareTo(java.math.BigDecimal.ZERO) > 0)
                .toList();

            return ProductAndRawMaterialDto.builder()
                    .id(rawMaterial.getId())
                    .name(rawMaterial.getMaterialName())
                    .code(rawMaterial.getMaterialCode())
                    .description(rawMaterial.getDescription())
                    .unitPrice(rawMaterial.getUnitCost())
                    .salePrice(rawMaterial.getUnitCost())
                    .category("raw_material")
                    .isActive(rawMaterial.getIsActive())
                    .type("raw_material")
                    .productionCenters(centerDtos)
                    .miniStoreAvailability(availabilityDtos)
                    .outletAvailability(new ArrayList<>())
                    .build();
        })
        .filter(dto -> dto.getMiniStoreAvailability() != null && !dto.getMiniStoreAvailability().isEmpty())
        .toList();
        
        result.addAll(productDtos);
        result.addAll(rawMaterialDtos);
        
        return ResponseEntity.ok(result);
    }

    // Mini Store Endpoints
    @GetMapping("/mini-stores")
    public ResponseEntity<List<MiniStoreDto>> getAllMiniStores() {
        log.info("Fetching all mini stores");
        try {
            List<MiniStoreDto> response = miniStoreService.getAllMiniStores();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching mini stores: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/mini-stores/{miniStoreId}/items")
    public ResponseEntity<List<MiniStoreItemDto>> getMiniStoreItems(@PathVariable Integer miniStoreId) {
        log.info("Fetching mini store items for store ID: {}", miniStoreId);
        try {
            List<MiniStoreItemDto> response = miniStoreService.getMiniStoreItems(miniStoreId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching mini store items: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/mini-stores/{miniStoreId}/items")
    public ResponseEntity<Void> updateMiniStoreItems(
            @PathVariable Integer miniStoreId,
            @Valid @RequestBody MiniStoreUpdateRequestDto updateRequest) {
        log.info("Updating mini store items for store ID: {}", miniStoreId);
        try {
            miniStoreService.updateMiniStoreItems(miniStoreId, updateRequest);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error updating mini store items: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Distribution Endpoints
    @GetMapping("/distribution/outlets")
    public ResponseEntity<List<OutletDto>> getAllOutlets() {
        log.info("Fetching all outlets");
        try {
            List<OutletDto> outlets = outletService.getAllOutlets();
            return ResponseEntity.ok(outlets);
        } catch (Exception e) {
            log.error("Error fetching outlets: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/distribution/plans")
    public ResponseEntity<List<DistributionPlanDto>> getAllDistributionPlans() {
        log.info("Fetching all distribution plans");
        try {
            // Use native query to handle invalid dates (date returned as string)
            List<Object[]> rawResults = distributionPlanRepository.findAllWithValidDatesNative();
            log.info("Fetched {} distribution plans using native query", rawResults.size());

            List<DistributionPlanDto> response = rawResults.stream()
                    .map(row -> {
                        try {
                            Long dpId = ((Number) row[0]).longValue();
                            String dateStr = (String) row[1];
                            Boolean isActive = (Boolean) row[2];
                            String name = (String) row[3];
                            Long outletId = ((Number) row[4]).longValue();
                            String status = row.length > 5 ? (String) row[5] : null;

                            // Convert date string to LocalDate safely
                            java.time.LocalDate localDate = null;
                            if (dateStr != null && !dateStr.isBlank()) {
                                try {
                                    localDate = java.time.LocalDate.parse(dateStr);
                                } catch (Exception pe) {
                                    log.warn("Unable to parse date '{}' for dpId {}", dateStr, dpId);
                                }
                            }

                            // Get outlet details
                            Outlet outlet = outletRepository.findById(outletId).orElse(null);

                            // Get distribution plan items
                            List<DistributionPlanItem> items = distributionPlanItemRepository.findByDistributionPlan_DpId(dpId);
                            List<DistributionPlanItemDto> itemDtos = items.stream()
                                    .map(item -> DistributionPlanItemDto.builder()
                                    .dpiId(item.getDpiId())
                                    .productId(item.getProduct().getId())
                                    .productName(item.getProduct().getProductName())
                                    .qty(item.getQty())
                                    .dpId(item.getDistributionPlan().getDpId())
                                    .build())
                                    .toList();

                            return DistributionPlanDto.builder()
                                    .dpId(dpId)
                                    .name(name)
                                    .outletId(outlet != null ? outlet.getOutletId() : outletId)
                                    .outletName(outlet != null ? outlet.getName() : "Unknown Outlet")
                                    .date(localDate)
                                    .isActive(isActive)
                                    .status(status)
                                    .items(itemDtos)
                                    .build();
                        } catch (Exception e) {
                            log.error("Error processing distribution plan row: {}", e.getMessage());
                            // Return a minimal DTO for problematic rows
                            return DistributionPlanDto.builder()
                                    .dpId(((Number) row[0]).longValue())
                                    .name((String) row[3])
                                    .outletId(((Number) row[4]).longValue())
                                    .outletName("Unknown Outlet")
                                    .date(null)
                                    .isActive((Boolean) row[2])
                                    .status(row.length > 5 ? (String) row[5] : null)
                                    .items(new ArrayList<>())
                                    .build();
                        }
                    })
                    .toList();

            log.info("Successfully processed {} distribution plans", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching distribution plans: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/distribution/plans")
    public ResponseEntity<DistributionPlanDto> createDistributionPlan(
            @Valid @RequestBody DistributionPlanRequestDto requestDto) {
        log.info("Creating new distribution plan: {}", requestDto.getName());

        try {
            // Find the outlet
            Outlet outlet = outletRepository.findById(requestDto.getOutletId())
                    .orElseThrow(() -> new RuntimeException("Outlet not found with ID: " + requestDto.getOutletId()));

            // Create the distribution plan
            DistributionPlan distributionPlan = DistributionPlan.builder()
                    .name(requestDto.getName())
                    .outlet(outlet)
                    .date(requestDto.getDate())
                    .isActive(requestDto.getIsActive())
                    .status(requestDto.getStatus() != null && !requestDto.getStatus().trim().isEmpty()
                            ? DistributionPlanStatus.fromString(requestDto.getStatus())
                            : DistributionPlanStatus.NOT_RECEIVED) // Default status
                    .build();

            // Save the distribution plan
            DistributionPlan savedPlan = distributionPlanRepository.save(distributionPlan);

            // Create and save distribution plan items
            List<DistributionPlanItem> planItems = requestDto.getItems().stream()
                    .map(itemDto -> {
                        Product product = productRepository.findById(itemDto.getProductId())
                                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + itemDto.getProductId()));

                        return DistributionPlanItem.builder()
                                .product(product)
                                .qty(itemDto.getQty())
                                .distributionPlan(savedPlan)
                                .build();
                    })
                    .toList();

            List<DistributionPlanItem> savedItems = distributionPlanItemRepository.saveAll(planItems);

            // Build response DTO
            List<DistributionPlanItemDto> itemDtos = savedItems.stream()
                    .map(item -> DistributionPlanItemDto.builder()
                    .dpiId(item.getDpiId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getProductName())
                    .qty(item.getQty())
                    .dpId(item.getDistributionPlan().getDpId())
                    .build())
                    .toList();

            DistributionPlanDto response = DistributionPlanDto.builder()
                    .dpId(savedPlan.getDpId())
                    .name(savedPlan.getName())
                    .outletId(savedPlan.getOutlet().getOutletId())
                    .outletName(savedPlan.getOutlet().getName())
                    .date(savedPlan.getDate())
                    .isActive(savedPlan.getIsActive())
                    .status(savedPlan.getStatus() != null ? savedPlan.getStatus().getValue() : null)
                    .items(itemDtos)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating distribution plan: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/distribution/plans/{dpId}")
    public ResponseEntity<Map<String, Object>> deleteDistributionPlan(@PathVariable Long dpId) {
        log.info("Received request to delete distribution plan: {}", dpId);

        try {
            // Validate dpId
            if (dpId == null || dpId <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid distribution plan ID. ID must be a positive number.");
                errorResponse.put("dpId", dpId);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check if distribution plan exists
            DistributionPlan distributionPlan = distributionPlanRepository.findById(dpId)
                    .orElse(null);

            if (distributionPlan == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Distribution plan not found with ID: " + dpId);
                errorResponse.put("dpId", dpId);
                return ResponseEntity.notFound().build();
            }

            log.info("Found distribution plan '{}' (ID: {}). Proceeding with deletion.",
                    distributionPlan.getName(), dpId);

            // Delete the distribution plan (items will be deleted automatically due to cascade)
            distributionPlanRepository.delete(distributionPlan);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Distribution plan and all related items deleted successfully");
            successResponse.put("dpId", dpId);
            successResponse.put("planName", distributionPlan.getName());
            successResponse.put("deletedAt", java.time.LocalDateTime.now());

            log.info("Successfully deleted distribution plan: {} (ID: {})",
                    distributionPlan.getName(), dpId);
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            log.error("Error deleting distribution plan {}: {}", dpId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An error occurred while deleting the distribution plan: " + e.getMessage());
            errorResponse.put("dpId", dpId);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/distribution/plans/{dpId}")
    @Transactional
    public ResponseEntity<DistributionPlanDto> updateDistributionPlan(
            @PathVariable Long dpId,
            @Valid @RequestBody DistributionPlanRequestDto requestDto) {
        log.info("Received request to update distribution plan: {}", dpId);

        try {
            // Validate dpId
            if (dpId == null || dpId <= 0) {
                throw new RuntimeException("Invalid distribution plan ID. ID must be a positive number.");
            }

            // Check if distribution plan exists
            DistributionPlan existingPlan = distributionPlanRepository.findById(dpId)
                    .orElseThrow(() -> new RuntimeException("Distribution plan not found with ID: " + dpId));

            log.info("Found distribution plan '{}' (ID: {}). Proceeding with update.",
                    existingPlan.getName(), dpId);

            // Find the outlet
            Outlet outlet = outletRepository.findById(requestDto.getOutletId())
                    .orElseThrow(() -> new RuntimeException("Outlet not found with ID: " + requestDto.getOutletId()));

            // Update the distribution plan
            existingPlan.setName(requestDto.getName());
            existingPlan.setOutlet(outlet);
            existingPlan.setDate(requestDto.getDate());
            existingPlan.setIsActive(requestDto.getIsActive());

            // Handle status field
            if (requestDto.getStatus() != null && !requestDto.getStatus().trim().isEmpty()) {
                try {
                    DistributionPlanStatus status = DistributionPlanStatus.fromString(requestDto.getStatus());
                    existingPlan.setStatus(status);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status value '{}' provided, keeping existing status", requestDto.getStatus());
                }
            }

            // Save the updated distribution plan (without touching items yet)
            DistributionPlan savedPlan = distributionPlanRepository.save(existingPlan);

            // Fetch existing items for the plan
            List<DistributionPlanItem> existingItems = distributionPlanItemRepository.findByDistributionPlan_DpId(dpId);

            // Build a map of existing items by productId for quick lookup
            java.util.Map<Long, DistributionPlanItem> existingByProductId = existingItems.stream()
                    .collect(java.util.stream.Collectors.toMap(i -> i.getProduct().getId(), i -> i));

            // Track changes
            java.util.List<DistributionPlanItem> itemsToUpdate = new java.util.ArrayList<>();
            java.util.List<DistributionPlanItem> itemsToCreate = new java.util.ArrayList<>();

            // Build a set of incoming productIds to identify deletions later
            java.util.Set<Long> incomingProductIds = new java.util.HashSet<>();

            for (DistributionPlanRequestDto.DistributionPlanItemRequestDto itemDto : requestDto.getItems()) {
                Long productId = itemDto.getProductId();
                incomingProductIds.add(productId);

                DistributionPlanItem existingItem = existingByProductId.get(productId);
                if (existingItem != null) {
                    // Update qty if changed
                    if (existingItem.getQty() == null || !existingItem.getQty().equals(itemDto.getQty())) {
                        existingItem.setQty(itemDto.getQty());
                        itemsToUpdate.add(existingItem);
                    }
                } else {
                    // Create new item for this product
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
                    DistributionPlanItem newItem = DistributionPlanItem.builder()
                            .product(product)
                            .qty(itemDto.getQty())
                            .distributionPlan(savedPlan)
                            .build();
                    itemsToCreate.add(newItem);
                }
            }

            // Items to delete: existing items whose productId not in incoming
            java.util.List<DistributionPlanItem> itemsToDelete = existingItems.stream()
                    .filter(item -> !incomingProductIds.contains(item.getProduct().getId()))
                    .toList();

            if (!itemsToDelete.isEmpty()) {
                distributionPlanItemRepository.deleteAll(itemsToDelete);
                distributionPlanItemRepository.flush();
                log.info("Deleted {} distribution plan items", itemsToDelete.size());
            }
            if (!itemsToUpdate.isEmpty()) {
                distributionPlanItemRepository.saveAll(itemsToUpdate);
                distributionPlanItemRepository.flush();
                log.info("Updated {} distribution plan items", itemsToUpdate.size());
            }
            java.util.List<DistributionPlanItem> createdItems = java.util.Collections.emptyList();
            if (!itemsToCreate.isEmpty()) {
                createdItems = distributionPlanItemRepository.saveAll(itemsToCreate);
                distributionPlanItemRepository.flush();
                log.info("Created {} new distribution plan items", createdItems.size());
            }

            // Re-fetch current items to build accurate response
            List<DistributionPlanItem> savedItems = distributionPlanItemRepository.findByDistributionPlan_DpId(dpId);

            // Build response DTO
            List<DistributionPlanItemDto> itemDtos = savedItems.stream()
                    .map(item -> DistributionPlanItemDto.builder()
                    .dpiId(item.getDpiId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getProductName())
                    .qty(item.getQty())
                    .dpId(item.getDistributionPlan().getDpId())
                    .build())
                    .toList();

            DistributionPlanDto response = DistributionPlanDto.builder()
                    .dpId(savedPlan.getDpId())
                    .name(savedPlan.getName())
                    .outletId(savedPlan.getOutlet().getOutletId())
                    .outletName(savedPlan.getOutlet().getName())
                    .date(savedPlan.getDate())
                    .isActive(savedPlan.getIsActive())
                    .status(savedPlan.getStatus() != null ? savedPlan.getStatus().getValue() : null)
                    .items(itemDtos)
                    .build();

            log.info("Successfully updated distribution plan: {} (ID: {})",
                    savedPlan.getName(), dpId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating distribution plan {}: {}", dpId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating distribution plan {}: {}", dpId, e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred while updating the distribution plan", e);
        }
    }

    /**
     * Approve or reject a stock adjustment. This endpoint allows managers to
     * approve or reject pending stock adjustments. For approved adjustments,
     * the raw material stock will be updated. URL: PUT
     * /api/manager/stock-adjustments/approval
     */
    @PutMapping("/stock-adjustments/approval")
    public ResponseEntity<StockAdjustmentApprovalResponseDto> processStockAdjustmentApproval(
            @Valid @RequestBody StockAdjustmentApprovalRequestDto request) {
        log.info("Processing stock adjustment approval for ID: {} with status: {}",
                request.getStockAdjustmentId(), request.getStatus());

        try {
            StockAdjustmentApprovalResponseDto response = stockAdjustmentApprovalService.processStockAdjustmentApproval(request);

            log.info("Successfully processed stock adjustment ID: {} with status: {}",
                    response.getStockAdjustmentId(), response.getStatus());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing stock adjustment approval: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/returns/{returnId}/items/status")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateReturnItemStatuses(
            @PathVariable Long returnId,
            @Valid @RequestBody UpdateReturnItemStatusRequestDto request) {
        log.info("Updating return item statuses for returnId: {} with {} items", returnId, request.getItems().size());

        if (!returnId.equals(request.getReturnId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Path returnId and body returnId do not match"));
        }

        try {
            List<Long> ids = request.getItems().stream().map(UpdateReturnItemStatusRequestDto.ItemStatusUpdate::getItemId).toList();
            // Assume all items have same desired status for bulk update; if mixed, split by status
            ReturnStatus status = request.getItems().get(0).getStatus();

            int updated = rawMaterialReturnItemRepository.bulkUpdateStatus(returnId, ids, status);
            log.info("Updated status to {} for {} items in return {}", status, updated, returnId);

            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("updatedCount", updated);
            body.put("status", status);
            body.put("returnId", returnId);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Error updating return item statuses for returnId {}: {}", returnId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Approve a purchase order by updating its status to 'Approved'.
     * URL: PUT /api/manager/purchase-orders/{requestId}/approve
    /**
     * Approves a specific Purchase Order that was flagged as PENDING,
     * triggering GRN creation for the storekeeper automatically.
     */
    @PutMapping("/purchase-orders/{poId}/approve")
    public ResponseEntity<com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderResponseDto> approvePurchaseOrder(@PathVariable Long poId) {
        log.info("Manager requested to approve Purchase Order: {}", poId);
        try {
            com.plover.backerymanagmentsystem.store_keeper.dto.CreatePurchaseOrderResponseDto response = 
                    purchaseOrderCreationService.approvePurchaseOrder(poId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving Purchase Order {}: {}", poId, e.getMessage(), e);
            throw new RuntimeException("Failed to approve Purchase Order: " + e.getMessage(), e);
        }
    }
}
