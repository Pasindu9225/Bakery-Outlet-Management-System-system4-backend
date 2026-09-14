package com.plover.backerymanagmentsystem.admin.service.impl;

import com.plover.backerymanagmentsystem.admin.dto.AdminBOMRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.AdminBOMResponseDto;
import com.plover.backerymanagmentsystem.admin.service.AdminBOMService;
import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeIngredientRepository;
import com.plover.backerymanagmentsystem.manager.repository.PackDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBOMServiceImpl implements AdminBOMService {

    private final BillOfMaterialRepository bomRepository;
    private final ProductRepository productRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final PackDetailsRepository packDetailsRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<AdminBOMResponseDto> getAllBOMs() {
        log.info("Fetching all bill of materials");
        List<BillOfMaterial> allBomItems = bomRepository.findAll();

        Map<Long, List<BillOfMaterial>> bomByParent = allBomItems.stream()
                .collect(Collectors.groupingBy(BillOfMaterial::getParentProductId));

        return bomByParent.entrySet().stream()
                .map(entry -> {
                    try {
                        return convertToResponseDto(entry.getKey(), entry.getValue());
                    } catch (Exception e) {
                        log.error("Error converting BOM for parent product ID: " + entry.getKey(), e);
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    private AdminBOMResponseDto convertToResponseDto(Long parentId, List<BillOfMaterial> items) {
        Product parentProduct = productRepository.findById(parentId).orElse(null);
        if (parentProduct == null) {
            log.warn("Parent product not found for ID: {}", parentId);
            return null;
        }

        List<AdminBOMResponseDto.BOMChildItemDto> childItemDtos = items.stream().map(item -> {
            AdminBOMResponseDto.BOMChildItemDto.BOMChildItemDtoBuilder childBuilder = AdminBOMResponseDto.BOMChildItemDto
                    .builder()
                    .id(item.getChildItemId())
                    .qty(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                    .unit(item.getUnit())
                    .active(item.getIsActive() != null ? item.getIsActive() : false);

            if (item.getProductionCenterId() != null) {
                productionCenterRepository.findById(item.getProductionCenterId())
                        .ifPresent(pc -> childBuilder.productionCenter(pc.getCenterName()));
            }

            if (item.getChildType() == BillOfMaterial.ChildType.raw_material) {
                RawMaterial rm = resolveRawMaterial(item.getChildItemId());
                if (rm != null) {
                    BigDecimal cost = rm.getUnitCost() != null ? BigDecimal.valueOf(rm.getUnitCost()) : BigDecimal.ZERO;
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    childBuilder.itemCode(rm.getMaterialCode())
                            .name(rm.getGenericMaterialName() != null ? rm.getGenericMaterialName() : rm.getMaterialName())
                            .type("Raw Material")
                            .cost(cost)
                            .unitPrice(cost) // Default to cost if not separately defined
                            .totalCost(cost.multiply(qty))
                            .salePrice(null) // Raw materials usually aren't sold directly
                            .expectedGP(BigDecimal.ZERO)
                            .actualGP(null);

                    if ("pack".equalsIgnoreCase(rm.getUnitOfMeasure())) {
                        packDetailsRepository.findByRawMaterialId(rm.getId()).ifPresent(pd -> {
                            childBuilder.unit(pd.getPackUom());
                        });
                    }
                }
            } else if (item.getChildType() == BillOfMaterial.ChildType.product) {
                productRepository.findById(item.getChildItemId()).ifPresent(p -> {
                    BigDecimal price = p.getUnitPrice() != null ? BigDecimal.valueOf(p.getUnitPrice()) : BigDecimal.ZERO;
                    BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    childBuilder.itemCode(p.getProductCode())
                            .name(p.getProductName())
                            .type("Product")
                            .cost(price)
                            .unitPrice(price)
                            .totalCost(price.multiply(qty))
                            .salePrice(price != null ? price : BigDecimal.ZERO)
                            .expectedGP(p.getGbMargin() != null ? BigDecimal.valueOf(p.getGbMargin()) : BigDecimal.ZERO)
                            .actualGP(p.getGbMargin() != null ? BigDecimal.valueOf(p.getGbMargin()) : BigDecimal.ZERO);
                });
            }

            return childBuilder.build();
        }).collect(Collectors.toList());

        // Calculate total cost and parent product metrics
        BigDecimal totalIngredientsCost = childItemDtos.stream()
                .map(item -> item.getTotalCost() != null ? item.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Double salePrice = parentProduct.getSalePrice() != null ? parentProduct.getSalePrice() : 0.0;
        BigDecimal actualGP = BigDecimal.ZERO;
        
        if (salePrice > 0) {
            // Margin % = (SP - Cost) / SP * 100
            BigDecimal sp = BigDecimal.valueOf(salePrice);
            actualGP = sp.subtract(totalIngredientsCost)
                    .divide(sp, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else if (totalIngredientsCost.compareTo(BigDecimal.ZERO) > 0) {
            // If SP is 0 but there is cost, it's a 100% loss relative to cost or just show as negative
            // The requirement says "negative value (a loss)". Setting to -100% or calculating based on cost.
            // Let's use -100 to indicate total loss of margin, or simply (0 - cost)
            actualGP = BigDecimal.valueOf(-100.0); 
        }

        return AdminBOMResponseDto.builder()
                .id(items.get(0).getId()) // Use first BOM entry ID as header ID reference
                .parentProduct(AdminBOMResponseDto.ParentProductDto.builder()
                        .id(parentProduct.getId())
                        .code(parentProduct.getProductCode())
                        .name(parentProduct.getProductName())
                        .category(parentProduct.getCategory())
                        .productionStage(parentProduct.getProductionStageRef() != null ? parentProduct.getProductionStageRef().getProductionStage() : null)
                        .salePrice(salePrice)
                        .expectedGP(parentProduct.getGbMargin() != null ? parentProduct.getGbMargin() : 0.0)
                        .actualGP(actualGP.doubleValue())
                        .totalCost(totalIngredientsCost.doubleValue())
                        .build())
                .createdDate(
                        parentProduct.getCreatedAt() != null ? parentProduct.getCreatedAt().format(DATE_FORMATTER)
                                : null)
                .status(parentProduct.getIsActive() != null && parentProduct.getIsActive() ? "Active" : "Inactive")
                .childItems(childItemDtos)
                .build();
    }

    @Override
    @Transactional
    public AdminBOMResponseDto saveBOM(AdminBOMRequestDto requestDto) {
        log.info("Saving Bill of Materials for parent product ID: {}", requestDto.getParentProductId());

        List<BillOfMaterial> bomItems = requestDto.getItems().stream().map(itemDto -> {
            if (itemDto.getChildItemId() == null) {
                log.error("Child item ID is missing for item: {}", itemDto);
                throw new IllegalArgumentException("Child item ID cannot be null. Please re-select the item.");
            }

            BillOfMaterial.ChildType childType;
            try {
                childType = BillOfMaterial.ChildType.valueOf(itemDto.getChildType());
            } catch (IllegalArgumentException e) {
                log.error("Invalid child type: {}", itemDto.getChildType());
                throw new IllegalArgumentException("Invalid child type: " + itemDto.getChildType());
            }

            return BillOfMaterial.builder()
                    .parentProductId(requestDto.getParentProductId())
                    .childItemId(itemDto.getChildItemId())
                    .childType(childType)
                    .quantity(itemDto.getQuantity())
                    .unit(itemDto.getUnit())
                    .productionCenterId(itemDto.getProductionCenterId())
                    .isActive(itemDto.getIsActive() != null ? itemDto.getIsActive() : true)
                    .build();
        }).collect(Collectors.toList());

        List<BillOfMaterial> savedItems = bomRepository.saveAll(bomItems);
        log.info("Successfully saved {} BOM items", savedItems.size());

        // Update product unit cost and Actual GB in database
        updateProductMetrics(requestDto.getParentProductId());

        return convertToResponseDto(requestDto.getParentProductId(), savedItems);
    }

    @Override
    @Transactional
    public void recalculateBOMMetrics(Long productId) {
        log.info("Recalculating BOM metrics for product ID: {}", productId);
        updateProductMetrics(productId);
    }

    private void updateProductMetrics(Long productId) {
        updateProductMetricsHelper(productId, new java.util.HashSet<>());
    }

    private void updateProductMetricsHelper(Long productId, java.util.Set<Long> visited) {
        if (productId == null || visited.contains(productId)) return;
        visited.add(productId);

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            visited.remove(productId);
            return;
        }

        List<BillOfMaterial> items = bomRepository.findByParentProductIdAndIsActiveTrue(productId);
        if (items.isEmpty()) {
            visited.remove(productId);
            return;
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        for (BillOfMaterial item : items) {
            BigDecimal itemCost = BigDecimal.ZERO;
            if (item.getChildType() == BillOfMaterial.ChildType.raw_material) {
                RawMaterial rm = resolveRawMaterial(item.getChildItemId());
                if (rm != null && rm.getUnitCost() != null) {
                    itemCost = BigDecimal.valueOf(rm.getUnitCost());
                }
            } else {
                Product childProd = productRepository.findById(item.getChildItemId()).orElse(null);
                if (childProd != null) {
                    List<BillOfMaterial> childBomItems = bomRepository.findByParentProductIdAndIsActiveTrue(childProd.getId());
                    if (!childBomItems.isEmpty()) {
                        updateProductMetricsHelper(childProd.getId(), visited);
                        childProd = productRepository.findById(childProd.getId()).orElse(childProd);
                    }
                    itemCost = BigDecimal.valueOf(childProd.getUnitPrice() != null ? childProd.getUnitPrice() : 0.0);
                }
            }
            totalCost = totalCost.add(itemCost.multiply(item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO));
        }

        product.setUnitPrice(totalCost.doubleValue());
        
        Double salePrice = product.getSalePrice() != null ? product.getSalePrice() : 0.0;
        if (salePrice > 0) {
            BigDecimal sp = BigDecimal.valueOf(salePrice);
            BigDecimal actualGP = sp.subtract(totalCost)
                    .divide(sp, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            product.setActualGP(actualGP.doubleValue());
        } else if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            product.setActualGP(-100.0);
        } else {
            product.setActualGP(0.0);
        }

        productRepository.save(product);

        List<BillOfMaterial> parentUsages = bomRepository.findByChildItemIdAndChildType(productId, BillOfMaterial.ChildType.product);
        for (BillOfMaterial parentBom : parentUsages) {
            if (parentBom.getParentProductId() != null && !visited.contains(parentBom.getParentProductId())) {
                updateProductMetricsHelper(parentBom.getParentProductId(), visited);
            }
        }

        visited.remove(productId);
    }

    @Override
    @Transactional
    public AdminBOMResponseDto updateBOM(Long parentProductId, AdminBOMRequestDto requestDto) {
        log.info("Updating Bill of Materials for parent product ID: {}", parentProductId);

        // Delete existing BOM items for this parent product and flush immediately
        bomRepository.deleteByParentProductId(parentProductId);
        bomRepository.flush();

        // Save new items using the saveBOM logic
        // We ensure the parentProductId from the path/param is used
        requestDto.setParentProductId(parentProductId);
        return saveBOM(requestDto);
    }

    @Override
    @Transactional
    public void deleteBOM(Long parentProductId) {
        log.info("Deleting Bill of Materials for parent product ID: {}", parentProductId);
        bomRepository.deleteByParentProductId(parentProductId);
        bomRepository.flush();
    }

    private RawMaterial resolveRawMaterial(Long id) {
        // 1. Try finding in raw_materials table directly
        RawMaterial rm = rawMaterialRepository.findById(id).orElse(null);
        if (rm != null) return rm;

        // 2. Fallback: Check if it's a recipe_ingredient ID
        log.debug("Raw material not found by ID {} in Admin BOM. Attempting fallback.", id);
        return recipeIngredientRepository.findById(id)
                .map(ingredient -> {
                    String name = ingredient.getRawMaterial() != null ? ingredient.getRawMaterial().getMaterialName() : null;
                    if (name == null) return null;
                    return rawMaterialRepository.findAll().stream()
                            .filter(m -> name.equalsIgnoreCase(m.getMaterialName()))
                            .findFirst()
                            .orElse(null);
                }).orElse(null);
    }
}
