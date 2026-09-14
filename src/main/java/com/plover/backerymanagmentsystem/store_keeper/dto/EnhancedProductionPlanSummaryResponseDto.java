package com.plover.backerymanagmentsystem.store_keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedProductionPlanSummaryResponseDto {
    
    private List<ProductionPlanWithBomDto> productionPlans;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionPlanWithBomDto {
        private Long planId;
        private String planName;
        private LocalDateTime productionDate;
        private String createdBy;
        private String remarks;
        private Double totalEstimatedCost;
        private String status;
        private List<ProductWithBomDto> products;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductWithBomDto {
        private Long productId;
        private String productName;
        private Integer plannedQuantity;
        private String productionCenter;
        private List<BomNodeDto> children;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BomNodeDto {
        private Long childItemId;
        private String childName;
        private String childType; // "product" or "raw_material"
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitCost; // Only for raw materials
        private BigDecimal totalCost; // Only for raw materials
        private String productionCenter;
        private java.util.List<MiniStoreAvailabilityDto> miniStoreAvailability;
        private LocalDate expireDate; // Only for raw materials
        private BigDecimal currentStock; // Only for raw materials
        private List<BomNodeDto> children;
    }
}
