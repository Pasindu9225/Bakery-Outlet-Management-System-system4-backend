package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated, per-supplier outstanding rollup as surfaced on the FR-FIN-02
 * outstanding-summary screen. One row per supplier; suppliers with a zero
 * total are skipped from the summary unless {@code includeAll=true}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOutstandingDto {

    /** Display id, e.g. {@code "SUP-001"}. */
    private String id;

    /** Raw supplier id used for drill-down lookups. */
    private Long supplierId;

    private String name;

    /**
     * Always {@code "Supplier"} for now. Reserved for future when the same
     * endpoint may surface debtors as well.
     */
    private String type;

    /** Sum of outstanding GRN balances, less purchase-return credits, never negative. */
    private BigDecimal totalOutstanding;

    /** Sum of outstanding amounts on GRNs whose due date has passed. */
    private BigDecimal overdueAmount;

    /** Count of GRNs with a positive outstanding balance. */
    private int unpaidInvoices;

    /** Earliest due date across all unpaid GRNs (ISO {@code YYYY-MM-DD}), nullable. */
    private LocalDate earliestDueDate;

    /** Latest of {@code GRN.receivedDate}, payment date, return date for this supplier. */
    private LocalDate lastTransaction;

    private String phone;
    private String email;
    private String address;
}
