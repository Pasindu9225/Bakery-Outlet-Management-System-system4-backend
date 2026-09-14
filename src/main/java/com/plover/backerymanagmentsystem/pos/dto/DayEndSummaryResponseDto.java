package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayEndSummaryResponseDto {

    private LocalDate closureDate;
    private UUID cashierId;
    
    // Expected System Totals
    private BigDecimal openingFloat;
    private BigDecimal expectedCash;
    private BigDecimal expectedCard;
    private BigDecimal expectedUber;
    private BigDecimal expectedPickme;
    
    // Aggregates
    private BigDecimal totalCashRefunds;
    private BigDecimal totalFreeMealsAmount;

    // Sales details
    private Integer totalBills;
    private BigDecimal totalSalesAmount;
    private Long loyaltyCustomersCount;
    private Long pendingWaiterItemsCount;
}
