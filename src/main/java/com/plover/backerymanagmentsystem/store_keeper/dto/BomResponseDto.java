package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomResponseDto {

    private Long parentProductId;
    private String parentProductName;
    private String parentProductCode;
    private Long productionCenterId;
    private String productionCenterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BomItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BomItemDto {
        private Long id;
        private String type; // raw_material | product
        private Long childId;
        private String childName;
        private String childCode;
        private String category; // from products.category for product children
        private java.math.BigDecimal quantity;
        private String unit;
        private Long productionCenterId;
        private String productionCenterName;
        private java.util.List<BomRawMaterialDto> rawMaterials; // flattened raw materials to produce this item
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BomRawMaterialDto {
        private Long rawMaterialId;
        private String rawMaterialName;
        private String rawMaterialCode;
        private java.math.BigDecimal quantity;
        private String unit;
        private Long productionCenterId;
        private String productionCenterName;
    }
}


