package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniStoreItemDto {
    
    private Integer itemId;
    private String name;
    private Long rawMaterialId;
    private BigDecimal systemQty;
    private BigDecimal physicalQty;
    private BigDecimal variance;
    private Integer miniStoreId;
    private Long outletId;
    private Long productId;
    private String brandName;
    private String genericMaterialName;
    private String unitOfMeasure;
    private String unit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
