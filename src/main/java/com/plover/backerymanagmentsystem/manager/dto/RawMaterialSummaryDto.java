package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialSummaryDto {

    private Long id;
    private String materialName;
    private String materialCode;
    private Double currentStock;
    private String category;
    private String unitOfMeasure;
    private Double unitCost;
}
