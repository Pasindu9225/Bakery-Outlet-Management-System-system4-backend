package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight projection of a GRN with an outstanding (unpaid) balance, as
 * surfaced on the supplier-payment screen. Aimed purely at the UI: dates are
 * encoded as ISO strings, status is the same {@code Pending}/{@code Overdue}
 * vocabulary used by the ledger.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutstandingGrnDto {

    /** Underlying GRN id (used as React key on the frontend). */
    private Long grnId;

    /** Display reference, e.g. {@code "GRN-12"}. */
    private String ref;

    /** Human-friendly description, e.g. {@code "Goods Received - INV-44"}. */
    private String description;

    /** GRN received date as {@code YYYY-MM-DD}. */
    private String date;

    /** Soft due date = received date + 30 days, as {@code YYYY-MM-DD}. */
    private String dueDate;

    /** GRN total amount. */
    private BigDecimal amount;

    /** Sum of allocated payments against this GRN. */
    private BigDecimal paid;

    /** {@code amount - paid}. Always positive in this projection. */
    private BigDecimal outstanding;

    /** {@code Overdue} if today &gt; dueDate AND outstanding &gt; 0, else {@code Pending}. */
    private String status;
}
