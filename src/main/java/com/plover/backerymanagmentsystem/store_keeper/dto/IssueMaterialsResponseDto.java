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
public class IssueMaterialsResponseDto {

    private String message;
    private Long productionPlanId;
    private String productionPlanName;
    private LocalDateTime issuanceDateTime;
    private List<IssuedMaterialDetail> issuedMaterials;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssuedMaterialDetail {

        private Long materialId;
        private String materialName;
        private String materialCode;
        private Double issuedQuantity;
        private String unitOfMeasure;
        private Double previousStock;
        private Double updatedStock;
        private Double unitCost;
        private Double totalCost;
        private Integer miniStoreId;
    }
}
