package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAndRawMaterialDto {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Double unitPrice;
    private Double salePrice;
    private String category;
    private Boolean isActive;
    private String type; // "product" or "raw_material"
    private Boolean isFastMoving;
    private List<ProductionCenterDto> productionCenters;
    private List<MiniStoreAvailabilityDto> miniStoreAvailability;
    private List<OutletAvailabilityDto> outletAvailability;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionCenterDto {
        private Long id;
        private String centerName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniStoreAvailabilityDto {
        private Integer miniStoreId;
        private String miniStoreName;
        private java.math.BigDecimal availableQty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutletAvailabilityDto {
        private Long outletId;
        private String outletName;
        private Integer availableQty;
    }
}
