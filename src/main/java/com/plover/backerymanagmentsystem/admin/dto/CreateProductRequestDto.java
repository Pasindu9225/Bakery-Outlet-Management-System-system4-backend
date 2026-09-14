package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequestDto {
    private String category;
    private String brand;
    private String productCode;
    private String productName;
    private Long productionCenterId;
    private Double unitPrice;
    private Double gbMargin;
    private Boolean isActive;
    private Boolean vatStatus;
    private String description;
    private Integer productionStageId;
    private Double salePrice;
    private Double actualGP;
    private Double maxOrderQty;
    private Double minOrderQty;
    private String unitOfMeasure;
    private Boolean isKotEnabled;
    private Boolean isFastMoving;
    private Integer shelfLifeDays;
}
