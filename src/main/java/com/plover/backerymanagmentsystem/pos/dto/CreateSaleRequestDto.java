package com.plover.backerymanagmentsystem.pos.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a sale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSaleRequestDto {

    @NotNull(message = "Cashier ID is required")
    private UUID cashierId;

    @NotEmpty(message = "Sale items cannot be empty")
    @Valid
    private List<SaleItemRequestDto> items;

    private Long tableId;
    private Long outletId;
    private Boolean skipKot;
    private Long globalPromotionId;
    private String deliveryOption;
    private String customerPhoneNumber;
    private Boolean redeemPoints;
    private String otp;
    private Boolean invoicePrinted;
}
