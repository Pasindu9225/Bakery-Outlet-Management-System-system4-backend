package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionCenterSummaryDto {
    
    private Long productionCenterId;
    private String productionCenterName;
    private List<ProductionPlanMaterialSummaryResponse.MaterialQuantitySummary> materials;
    private List<ProductSummary> products;
    private Double totalCost;
}
