package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanMaterialsDto {

    private Long id;
    private Long productionPlanId;
    private Long rawMaterialId;
    private String rawMaterialName;
    private Double totalRawMaterialQuantity;
    private Double rawMaterialQuantityForKitchen;
    private Double rawMaterialQuantityForBakery;
}


