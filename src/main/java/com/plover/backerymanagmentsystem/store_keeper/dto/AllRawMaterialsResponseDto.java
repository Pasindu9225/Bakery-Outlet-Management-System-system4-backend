package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for retrieving all available raw materials with current stock >
 * 0. Contains a list of raw materials with their basic information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllRawMaterialsResponseDto {

    private List<RawMaterialDto> rawMaterials;
    private Integer totalCount;

    /**
     * DTO representing basic raw material information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawMaterialDto {

        private Long id;
        private String materialName;
        private String materialCode;
        private String description;
        private String unitOfMeasure;
        private Double unitCost;
        private Double currentStock;
        private Double minimumStockLevel;
        private String category;
        private String brand;
        private String genericMaterialName;
        private String brandName;
        private Boolean isActive;
        private LocalDate expireDate;
    }
}
