package com.plover.backerymanagmentsystem.store_keeper.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReduceRawMaterialStockRequestDto {

    @NotNull(message = "Material ID is required")
    private Long materialId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;

    private String reason; // Optional reason for stock reduction
}
