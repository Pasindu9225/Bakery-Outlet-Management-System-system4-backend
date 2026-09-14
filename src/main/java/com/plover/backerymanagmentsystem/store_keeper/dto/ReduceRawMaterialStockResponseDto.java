package com.plover.backerymanagmentsystem.store_keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReduceRawMaterialStockResponseDto {

    private Long materialId;
    private String materialName;
    private String materialCode;
    private Double previousStock;
    private Double reducedQuantity;
    private Double newStock;
    private String unitOfMeasure;
    private Double minimumStockLevel;
    private Boolean belowMinimumLevel;
    private String message;
}
