package com.plover.backerymanagmentsystem.manager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanItemRequestDto {

    // Product ID - required when type is "product"
    private Long productId;

    // Raw Material ID - required when type is "raw_material"
    private Long rawMaterialId;

    @NotNull(message = "Item name is required")
    private String productName; // This will contain either product name or raw material name

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private Long productionCenterId; // Optional, can be null

    @NotNull(message = "Type is required")
    @Pattern(regexp = "^(product|raw_material)$", message = "Type must be either 'product' or 'raw_material'")
    private String type;

    private java.util.List<OutletAllocationRequestDto> outlets;
}
