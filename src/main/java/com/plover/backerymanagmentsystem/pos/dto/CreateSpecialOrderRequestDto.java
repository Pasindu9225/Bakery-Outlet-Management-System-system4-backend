package com.plover.backerymanagmentsystem.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new Special Order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSpecialOrderRequestDto {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer contact number is required")
    private String customerContact;

    private String customerEmail;
    private String customerAddress;

    @NotNull(message = "Items list cannot be null")
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<SpecialOrderItemRequestDto> items;

    @NotNull(message = "Advance amount is required")
    @DecimalMin(value = "0.01", message = "Advance must be greater than 0")
    private BigDecimal advanceAmount;

    @NotNull(message = "Delivery date is required")
    @FutureOrPresent(message = "Delivery date must be today or in the future")
    private LocalDate deliveryDate;

    private String notes;

    @NotNull(message = "Cashier ID is required")
    private UUID cashierId;

    @NotNull(message = "Outlet ID is required")
    private Long outletId;

    @NotBlank(message = "Manager verification code is required")
    private String managerVerificationCode;
}
