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
public class OpenCashFloatRequestDto {
    @NotNull(message = "Cashier ID is required")
    private UUID cashierId;
    
    @NotNull(message = "Outlet ID is required")
    private Integer outletId;
    
    @NotNull(message = "Opening balance is required")
    @PositiveOrZero(message = "Opening balance must be zero or positive")
    private BigDecimal openingBalance;
}
