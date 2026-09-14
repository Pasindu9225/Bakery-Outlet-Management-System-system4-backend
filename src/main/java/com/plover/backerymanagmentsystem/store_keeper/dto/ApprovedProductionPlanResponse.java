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
public class ApprovedProductionPlanResponse {

    private List<ProductionPlanDTO> productionPlans;
    private List<TotalRawMaterialDTO> totalRawMaterialRequirements;
    private List<ProductionCenterRequirementDTO> productionCenterRequirements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionPlanDTO {

        private Long planId;
        private String planName;
        private LocalDateTime planDate;
        private String status;
        private Double totalEstimatedCost;
        private String notes;
        private List<ProductionPlanItemDTO> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionPlanItemDTO {

        private Long productId;
        private String productName;
        private Integer quantity;
        private Double totalCost;
        private Long productionCenterId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalRawMaterialDTO {

        private Long materialId;
        private String materialName;
        private Double totalQuantity;
        private Double totalCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionCenterRequirementDTO {

        private Long productionCenterId;
        private List<RawMaterialDTO> rawMaterials;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawMaterialDTO {

        private Long materialId;
        private String materialName;
        private Double quantity;
        private Double totalCost;
    }
}
