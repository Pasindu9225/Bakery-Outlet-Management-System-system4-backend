package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRawMaterialRequestDto {

    private String category;
    private String brand;
    private String materialCode;
    private String materialName;
    
    // Alias for 'name'
    @com.fasterxml.jackson.annotation.JsonProperty("name")
    public void setName(String name) {
        this.materialName = name;
    }
    
    public String getName() {
        return materialName;
    }

    private String unitOfMeasure;
    private Double maxStockLevel;
    private Double minimumStockLevel;
    
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private Boolean isActive;
    
    // Alias for 'activeStatus'
    @com.fasterxml.jackson.annotation.JsonProperty("activeStatus")
    public void setActiveStatus(Boolean activeStatus) {
        this.isActive = activeStatus;
    }

    public Boolean getActiveStatus() {
        return isActive;
    }

    private Boolean vatIncluded;
    private List<PackDetailsDto> packDetails;
    private Double unitCost;
    private Long supplierId;
    private Long brandId;
    private Long genericMaterialId;
}
