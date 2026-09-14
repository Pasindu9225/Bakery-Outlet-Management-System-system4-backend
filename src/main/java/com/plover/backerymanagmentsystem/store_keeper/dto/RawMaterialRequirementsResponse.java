package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialRequirementsResponse {

    private List<RawMaterialRequirementDTO> rawMaterialRequirements;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawMaterialRequirementDTO {

        private Long materialId;
        private String materialName;
        private String materialCode;
        private Double totalQuantity;
        private Double totalCost;
        private Double bakeryQuantity;
        private Double kitchenQuantity;
        private String unitOfMeasure;
        private List<BatchRequirementDTO> batches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRequirementDTO {
        private Long id;
        private String batchNo;
        private Double currentStock;
        private LocalDate expireDate;
        private Double unitCost;
    }
}
