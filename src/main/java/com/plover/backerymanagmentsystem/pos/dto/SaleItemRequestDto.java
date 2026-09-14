package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sale item information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemRequestDto {

    private Integer dayProductionItemId;

    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer qty;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price must be greater than or equal to 0")
    private BigDecimal unitPrice;

    private String freeMealReason;

    private String bankTransferCode;

    private Integer discountId;

    private Long promotionId;

    @NotNull(message = "Payment method ID is required")
    private Integer paymentMethodId;

    @DecimalMin(value = "0.00", message = "Manual discount must be 0 or greater")
    private BigDecimal manualDiscount;

    private String specialInstructions;
}
