package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.plover.backerymanagmentsystem.manager.model.ProductionPlan.ProductionPlanStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanMaterialSummaryResponse {

    private List<ProductionPlanMaterialDetails> productionPlanSummaries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionPlanMaterialDetails {

        private Long productionPlanId;
        private String planName;
        private LocalDateTime planDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private ProductionPlanStatus status;
        private List<ProductionCenterSummaryDto> productionCenterSummaries;
        private Double totalPlanCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialQuantitySummary {

        private Long materialId;
        private String materialName;
        private Double kitchenQuantity;
        private Double bakeryQuantity;
        private Double totalQuantity;
        private String unitMeasure;
        private Double totalCost;
        private LocalDate expireDate;
        private java.util.List<MiniStoreAvailabilityDto> miniStoreAvailability;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummary {

        private Long productId;
        private String productName;
        private Integer plannedQuantity;
        private Double exactPlannedQuantity;
        private Integer completedQuantity;
        private Double exactCompletedQuantity;
        private Double unitCost;
        private Double totalCost;
        private Double rawMaterialCost;
    }
}
