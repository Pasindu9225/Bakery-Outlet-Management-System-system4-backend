package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandDto {
    private Long id;

    @com.fasterxml.jackson.annotation.JsonProperty("name")
    private String name;

    @com.fasterxml.jackson.annotation.JsonProperty("genericMaterialId")
    private Long genericMaterialId;

    @com.fasterxml.jackson.annotation.JsonProperty("genericMaterialName")
    private String genericMaterialName;

    // Frontend alias for robust mapping
    public void setBrandName(String brandName) {
        this.name = brandName;
    }
}
