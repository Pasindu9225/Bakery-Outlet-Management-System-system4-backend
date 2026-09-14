package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDetailsDto {

    private Long supplierId;
    private String name;
    private String address;
    private String contactNumber;
    private String email;
    private String repName;
    private String repContactNo;
    private String bankDetails;
    private String vatStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RawMaterialInfoDto> rawMaterials;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawMaterialInfoDto {
        private Long rawMaterialId;
        private String materialName;
        private String materialCode;
        private String category;
        private String brand;
        private Double negotiatedUnitCost;
        private Integer leadTimeDays;
        private Boolean isPreferred;
    }
}

