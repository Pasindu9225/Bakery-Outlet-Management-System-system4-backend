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
public class ProductWithCentersDto {
    private Long id;
    private String productName;
    private String productCode;
    private String description;
    private Double unitPrice;
    private String category;
    private Boolean isActive;
    private Boolean isFastMoving;
    private List<ProductionCenterDto> productionCenters;
    private List<MiniStoreAvailabilityDto> miniStoreAvailability;

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
}
