package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for retrieving raw materials supplied by a specific supplier.
 * Contains supplier information and list of raw materials they can supply.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRawMaterialsResponseDto {

    private Long supplierId;
    private String supplierName;
    private List<SupplierRawMaterialDto> rawMaterials;
    private Integer totalCount;

    /**
     * DTO representing raw material information supplied by a supplier.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierRawMaterialDto {

        private Long rawMaterialId;
        private String rawMaterialName;
        private LocalDate expireDate;
        private Double currentStock;
        private String materialCode;
        private String category;
        private String brand;
        private String unitOfMeasure;
        private Double unitCost;
        private Double negotiatedUnitCost;
        private Integer leadTimeDays;
        private Boolean isPreferred;
        private String batchNo;
    }
}
