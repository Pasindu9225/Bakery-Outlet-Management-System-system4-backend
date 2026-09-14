package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single row of a supplier ledger as returned to the UI. Each entry captures a
 * GRN, PO, payment, return or manual adjustment along with the running balance
 * after that entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryDto {

    /** Stable id used by the UI for keys (e.g. {@code "L-GRN-12"}). */
    private String id;

    private LocalDate date;

    /** Human-friendly reference number ({@code GRN-12}, {@code PMT-2026-001}). */
    private String ref;

    /** One of: {@code GRN}, {@code PO}, {@code Payment}, {@code Return}, {@code Adjustment}. */
    private String type;

    private String description;

    private BigDecimal debit;

    private BigDecimal credit;

    private BigDecimal balance;

    /** One of: {@code Pending}, {@code Cleared}, {@code Overdue}. */
    private String status;
}
