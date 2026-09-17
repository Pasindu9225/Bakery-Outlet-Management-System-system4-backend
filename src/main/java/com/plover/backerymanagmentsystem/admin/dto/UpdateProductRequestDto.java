package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequestDto {
    private String category;
    private String brand;
    private String productCode;
    private String productName;
    private Long productionCenterId;
    private Double unitPrice;
    private Double gbMargin;
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private Boolean isActive;
    private Boolean vatStatus;
    private String description;
    private Integer productionStageId;
    private Double salePrice;
    private Double actualGP;
    private Double maxOrderQty;
    private Double minOrderQty;
    private String unitOfMeasure;
    @com.fasterxml.jackson.annotation.JsonProperty("isKotEnabled")
    private Boolean isKotEnabled;
    @com.fasterxml.jackson.annotation.JsonProperty("isFastMoving")
    private Boolean isFastMoving;
    private Integer shelfLifeDays;
}
