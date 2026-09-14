package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftClosureRequestDto {

    @NotNull(message = "Closure date is required")
    private LocalDate closureDate;

    @NotNull(message = "Outlet ID is required")
    private Integer outletId;

    @NotNull(message = "Cashier ID is required")
    private java.util.UUID cashierId;

    @NotNull(message = "Actual cash count is required")
    private BigDecimal actualCash;

    @NotNull(message = "Actual card count is required")
    private BigDecimal actualCard;

    @NotNull(message = "Actual Uber total is required")
    private BigDecimal actualUber;

    @NotNull(message = "Actual Pickme total is required")
    private BigDecimal actualPickme;

    @NotNull(message = "Cashier PIN is required for verification")
    private String cashierPin;

    private Boolean shiftOnly;

    private String cashDenominations;
}
