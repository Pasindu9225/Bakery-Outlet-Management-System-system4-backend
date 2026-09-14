package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanItemResponseDto {
    
    private Long id;
    private Long productId;
    private Long rawMaterialId;
    private String productName; // This will contain either product name or raw material name
    private Integer quantity;
    private Double unitCost;
    private Double totalCost;
    private Double estimatedRawMaterialCost;
    private String type; // "product" or "raw_material"
    private Boolean isTopLevel;
    private Long parentPlanItemId;
    private Long productionCenterId;
    private Double reservedFromMiniStore;
    private Boolean miniStoreFulfilled;
    private String unitOfMeasure;
    private Double exactQuantity;
}
