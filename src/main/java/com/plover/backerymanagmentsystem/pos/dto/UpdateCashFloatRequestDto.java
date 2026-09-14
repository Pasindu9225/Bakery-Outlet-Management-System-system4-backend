package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCashFloatRequestDto {
    @NotNull(message = "Cashier ID is required")
    private UUID cashierId;
    
    @NotNull(message = "New balance is required")
    @PositiveOrZero(message = "New balance must be zero or positive")
    private BigDecimal newOpeningBalance;
}
