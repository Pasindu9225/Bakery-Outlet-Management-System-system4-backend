package com.plover.backerymanagmentsystem.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRawMaterialRequestDto {

    @com.fasterxml.jackson.annotation.JsonProperty("category")
    private String category;

    @com.fasterxml.jackson.annotation.JsonProperty("brand")
    private String brand;

    @NotBlank(message = "Material Code is required")
    private String materialCode;

    @com.fasterxml.jackson.annotation.JsonProperty("materialName")
    private String materialName;

    // Backward compatibility for 'name' field
    @com.fasterxml.jackson.annotation.JsonProperty("name")
    public void setName(String name) {
        this.materialName = name;
    }

    public String getName() {
        return materialName;
    }

    @NotBlank(message = "Unit of Measure is required")
    private String unitOfMeasure;

    private Long brandId;
    
    private Long genericMaterialId;

    public void setBrandName(String brandName) {
        this.brand = brandName;
    }

    private Double maxStockLevel;

    private Double minimumStockLevel;

    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private Boolean isActive;

    // Backward compatibility for 'activeStatus'
    @com.fasterxml.jackson.annotation.JsonProperty("activeStatus")
    public void setActiveStatus(Boolean activeStatus) {
        this.isActive = activeStatus;
    }

    private Boolean vatIncluded;

    private List<PackDetailsDto> packDetails;
    
    private Double unitCost;

    private Long supplierId;
}
