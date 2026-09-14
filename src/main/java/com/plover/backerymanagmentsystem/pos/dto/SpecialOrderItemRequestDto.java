package com.plover.backerymanagmentsystem.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for individual items in a Special Order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialOrderItemRequestDto {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price must be 0 or greater")
    private BigDecimal unitPrice;

    private String specialInstructions;
}
