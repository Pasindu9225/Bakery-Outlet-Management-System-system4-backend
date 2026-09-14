package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single ledger row exposed via {@code GET /api/v1/mis/suppliers/{id}/transactions}.
 *
 * <p>Shape mirrors {@link com.plover.backerymanagmentsystem.finance.dto.LedgerEntryDto}
 * — we wrap rather than expose the finance DTO directly so the MIS contract can
 * evolve independently without leaking finance-internal fields.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisSupplierTransactionDto {

    private String id;

    private LocalDate date;

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
