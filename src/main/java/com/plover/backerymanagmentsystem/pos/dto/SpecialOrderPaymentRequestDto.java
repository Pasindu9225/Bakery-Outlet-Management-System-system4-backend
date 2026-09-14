package com.plover.backerymanagmentsystem.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for recording a payment against a Special Order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialOrderPaymentRequestDto {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment method ID is required")
    private Integer paymentMethodId;

    @NotNull(message = "Cashier ID is required")
    private UUID cashierId;

    private String notes;
}
