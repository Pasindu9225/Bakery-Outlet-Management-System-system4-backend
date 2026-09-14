package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeIngredientRepository;
import com.plover.backerymanagmentsystem.manager.repository.RecipeRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.BomResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.service.BomService;
import com.plover.backerymanagmentsystem.store_keeper.dto.BomTreeResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BomServiceImpl implements BomService {

    private final BillOfMaterialRepository billOfMaterialRepository;
    private final ProductRepository productRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeRepository recipeRepository;

    @Override
    public BomResponseDto getBomByParentProductId(Long parentProductId) {
        Product parent = productRepository.findById(parentProductId).orElse(null);
        List<BillOfMaterial> rows = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(parentProductId);

        Map<Long, String> centerNames = productionCenterRepository.findAll().stream()
                .collect(Collectors.toMap(ProductionCenter::getId, ProductionCenter::getCenterName));

        List<BomResponseDto.BomItemDto> items = rows.stream().map(bom -> {
            String type = bom.getChildType().name();
            String name = null;
            String code = null;
            String category = null;
            if (bom.getChildType() == BillOfMaterial.ChildType.product) {
                Product p = productRepository.findById(bom.getChildItemId()).orElse(null);
                if (p != null) {
                    name = p.getProductName();
                    code = p.getProductCode();
                    category = p.getCategory();
                }
            } else {
                // Use the same raw material resolution logic as buildChildren method
                RawMaterial rm = resolveRawMaterial(bom.getChildItemId(), bom.getParentProductId());
                if (rm != null) {
                    name = rm.getDisplayName();
                    code = rm.getMaterialCode();
                }
            }

            // If child is a product (finished or semi), fetch its raw materials recursively via BOM rows
            List<BomResponseDto.BomRawMaterialDto> rawMaterials = new ArrayList<>();
            if (bom.getChildType() == BillOfMaterial.ChildType.product) {
                collectRawMaterialsRecursive(bom.getChildItemId(), rawMaterials, centerNames, new java.util.HashSet<>());
            }

            return BomResponseDto.BomItemDto.builder()
                    .id(bom.getId())
                    .type(type)
                    .childId(bom.getChildItemId())
                    .childName(name)
                    .childCode(code)
                    .category(category)
                    .quantity(bom.getQuantity())
                    .unit(bom.getUnit())
                    .productionCenterId(bom.getProductionCenterId())
                    .productionCenterName(centerNames.get(bom.getProductionCenterId()))
                    .rawMaterials(rawMaterials)
                    .build();
        }).toList();

        return BomResponseDto.builder()
                .parentProductId(parentProductId)
                .parentProductName(parent != null ? parent.getProductName() : null)
                .parentProductCode(parent != null ? parent.getProductCode() : null)
                .productionCenterId(parent != null ? parent.getProductionCenterId() : null)
                .productionCenterName(parent != null ? centerNames.get(parent.getProductionCenterId()) : null)
                .createdAt(parent != null ? parent.getCreatedAt() : null)
                .updatedAt(parent != null ? parent.getUpdatedAt() : null)
                .items(items)
                .build();
    }

    @Override
    public BomTreeResponseDto getBomTree(Long parentProductId) {
        Product parent = productRepository.findById(parentProductId).orElse(null);
        String parentName = parent != null ? parent.getProductName() : null;
        return BomTreeResponseDto.builder()
                .productId(parentProductId)
                .productName(parentName)
                .children(buildChildren(parentProductId))
                .build();
    }

    private List<BomTreeResponseDto.BomNodeDto> buildChildren(Long parentProductId) {
        Map<Long, String> centerNames = productionCenterRepository.findAll().stream()
                .collect(Collectors.toMap(ProductionCenter::getId, ProductionCenter::getCenterName));
        List<BillOfMaterial> rows = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(parentProductId);
        return rows.stream().map(row -> {
            String childName;
            Long childItemId = row.getChildItemId(); // Default to original child_item_id
            
            if (row.getChildType() == BillOfMaterial.ChildType.product) {
                Product prod = productRepository.findById(row.getChildItemId()).orElse(null);
                childName = prod != null ? prod.getProductName() : null;
            } else {
                // Use the centralized raw material resolution logic
                RawMaterial rm = resolveRawMaterial(row.getChildItemId(), row.getParentProductId());
                childName = rm != null ? rm.getDisplayName() : "Unknown";
                // For raw materials, use the resolved raw material ID as childItemId
                if (rm != null) {
                    childItemId = rm.getId();
                } else {
                    log.warn("Raw material not resolved for childItemId: {}, parentProductId: {}", row.getChildItemId(), row.getParentProductId());
                }
            }

            List<BomTreeResponseDto.BomNodeDto> grandChildren = row.getChildType() == BillOfMaterial.ChildType.product
                    ? buildChildren(row.getChildItemId())
                    : java.util.Collections.emptyList();

            return BomTreeResponseDto.BomNodeDto.builder()
                    .childItemId(childItemId) // Use the corrected childItemId
                    .childName(childName)
                    .childType(row.getChildType().name())
                    .quantity(row.getQuantity())
                    .unit(row.getUnit())
                    .productionCenter(centerNames.get(row.getProductionCenterId()))
                    .children(grandChildren)
                    .build();
        }).toList();
    }

    /**
     * Resolves raw material from bill_of_materials.child_item_id using the same logic as buildChildren method.
     * This ensures consistent raw material ID resolution across all BOM methods.
     */
    private RawMaterial resolveRawMaterial(Long childItemId, Long parentProductId) {
        // Prefer interpreting BOM.child_item_id as raw_materials.id to avoid mismatches (e.g., Flour vs Margarine)
        com.plover.backerymanagmentsystem.manager.model.Recipe recipe = recipeRepository
                .findByProductIdAndIsActiveTrue(parentProductId)
                .orElse(null);

        RawMaterial rm = null;

        // Attempt 1 (preferred): treat child_item_id as raw_materials.id, validate against recipe if available
        final Long initialRawMaterialId = childItemId;
        boolean allowed = true;
        if (recipe != null && recipe.getRecipeIngredients() != null) {
            allowed = recipe.getRecipeIngredients().stream()
                    .anyMatch(ing -> ing.getRawMaterial() != null
                            && ing.getRawMaterial().getId() != null
                            && ing.getRawMaterial().getId().equals(initialRawMaterialId));
        }
        if (allowed) {
            rm = rawMaterialRepository.findById(initialRawMaterialId).orElse(null);
        }

        // Attempt 2 (fallback): interpret child_item_id as recipe_ingredients.id → raw_material
        if (rm == null) {
            com.plover.backerymanagmentsystem.manager.model.RecipeIngredient riById = recipeIngredientRepository
                    .findById(childItemId)
                    .orElse(null);
            if (riById != null && riById.getRawMaterial() != null) {
                boolean belongsToRecipe = recipe == null || (riById.getRecipe() != null && riById.getRecipe().getId() != null
                        && riById.getRecipe().getId().equals(recipe.getId()));
                if (belongsToRecipe) {
                    rm = rawMaterialRepository.findById(riById.getRawMaterial().getId()).orElse(null);
                }
            }
        }

        return rm;
    }

    private void collectRawMaterialsRecursive(Long productId, List<BomResponseDto.BomRawMaterialDto> rawMaterials, Map<Long, String> centerNames, java.util.Set<Long> visited) {
        if (productId == null || visited.contains(productId)) return;
        visited.add(productId);

        List<BillOfMaterial> childRows = billOfMaterialRepository.findByParentProductIdAndIsActiveTrue(productId);
        for (BillOfMaterial childBom : childRows) {
            if (childBom.getChildType() == BillOfMaterial.ChildType.raw_material) {
                RawMaterial childRm = resolveRawMaterial(childBom.getChildItemId(), childBom.getParentProductId());
                if (childRm != null) {
                    rawMaterials.add(BomResponseDto.BomRawMaterialDto.builder()
                            .rawMaterialId(childRm.getId())
                            .rawMaterialName(childRm.getDisplayName())
                            .rawMaterialCode(childRm.getMaterialCode())
                            .quantity(childBom.getQuantity())
                            .unit(childBom.getUnit())
                            .productionCenterId(childBom.getProductionCenterId())
                            .productionCenterName(centerNames.get(childBom.getProductionCenterId()))
                            .build());
                }
            } else if (childBom.getChildType() == BillOfMaterial.ChildType.product) {
                collectRawMaterialsRecursive(childBom.getChildItemId(), rawMaterials, centerNames, visited);
            }
        }
    }
}



