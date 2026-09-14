package com.plover.backerymanagmentsystem.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupplierRequestDto {

    @NotBlank(message = "Name is required")
    private String name;
    
    private String address;
    private String contactNumber;
    private String email;
    private String repName;
    private String repContactNo;
    private String bankDetails;
    private String vatStatus;
    
    @Valid
    private List<RawMaterialSupplierDto> rawMaterials;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawMaterialSupplierDto {
        private Long rawMaterialId;
        private Double negotiatedUnitCost;
        private Integer leadTimeDays;
        private Boolean isPreferred;
    }
}

