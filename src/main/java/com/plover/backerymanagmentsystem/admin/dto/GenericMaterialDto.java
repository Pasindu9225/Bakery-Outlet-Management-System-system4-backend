package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericMaterialDto {
    private Long id;
    private String name;
    private String category;
    private String unitOfMeasure;
    private Integer categoryId; // support ID mapping
    private Double minimumStockLevel;
    private Double maxStockLevel;
    private String description;

    // Frontend aliases
    public void setGenericMaterialName(String genericMaterialName) {
        this.name = genericMaterialName;
    }

    public void setCategoryName(String categoryName) {
        this.category = categoryName;
    }
}
