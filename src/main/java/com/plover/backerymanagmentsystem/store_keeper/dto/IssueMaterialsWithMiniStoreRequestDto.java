package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueMaterialsWithMiniStoreRequestDto {

    @NotNull(message = "Production plan ID is required")
    @Positive(message = "Production plan ID must be positive")
    private Long productionPlanId;

    @NotEmpty(message = "Mini store materials cannot be empty")
    @Valid
    private List<MiniStoreMaterialsDto> miniStoreMaterials;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniStoreMaterialsDto {
        @NotNull(message = "Mini store ID is required")
        private Integer miniStoreId;

        @NotEmpty(message = "Issued materials cannot be empty")
        @Valid
        private List<IssuedMaterialDto> issuedMaterials;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssuedMaterialDto {
        @NotNull(message = "Raw material ID is required")
        private Long rawMaterialId;

        @NotNull(message = "Material name is required")
        private String materialName;

        @NotNull(message = "Unit of measure is required")
        private String unitOfMeasure;

        @NotNull(message = "Issued quantity is required")
        private BigDecimal issuedQty;

        @NotNull(message = "Unit cost is required")
        private BigDecimal unitCost;
    }
}


