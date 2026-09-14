package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialRequirementDto {
    
    private Long id;
    private Long rawMaterialId;
    private String rawMaterialName;
    private String materialCode;
    private Double requiredQuantity;
    private String unitOfMeasure;
    private Double unitCost;
    private Double totalCost;
    private Double availableStock;
    private Double stockDeficit;
    private String productionCenterName;
    private Long productionCenterId;
}
