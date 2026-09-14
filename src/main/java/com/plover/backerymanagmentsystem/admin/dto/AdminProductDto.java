package com.plover.backerymanagmentsystem.admin.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductDto {
    private Long id;
    private String productName;
    private String productCode;
    private String description;
    private Double unitPrice;
    private String categoryName;
    private Boolean isActive;
    private Long productionCenterId;
    private String productionCenterName;
    private Double gbMargin;
    private Boolean vatStatus;
    private String brand;
    private Integer productionStageId;
    private String productionStageName;
    private Double salePrice;
    private Double actualGP;
    private Double maxOrderQty;
    private Double minOrderQty;
    private String unitOfMeasure;
    private Boolean isKotEnabled;
    private Boolean isFastMoving;
    private Integer shelfLifeDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
