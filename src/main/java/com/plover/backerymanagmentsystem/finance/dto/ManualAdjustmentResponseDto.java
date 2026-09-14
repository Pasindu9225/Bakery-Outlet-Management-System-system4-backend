package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload returned after creating a manual ledger adjustment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAdjustmentResponseDto {

    private Long adjustmentId;

    private String adjustmentRef;

    private Long supplierId;

    private LocalDate date;

    private BigDecimal amount;

    private String description;

    private String remarks;
}
