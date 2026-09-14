package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for raw material suppliers information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialSuppliersResponseDto {

    private Long rawMaterialId;
    private String rawMaterialName;
    private List<SupplierInfoDto> suppliers;
    private Integer totalSuppliersCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierInfoDto {

        private Long supplierId;
        private String name;
        private String address;
        private String contactNumber;
        private String email;
        private Double negotiatedUnitCost;
        private Integer leadTimeDays;
        private Boolean isPreferred;
    }
}
