package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Joined PO / GRN row for the "Purchase &amp; Delivery Records" tab on the
 * FR-MIS-02 detail modal. One row per purchase order; if the PO has not been
 * received yet, {@code grnNo} and {@code grnDate} are {@code "—"} / {@code null}
 * and {@code status} is {@code "Pending"}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisSupplierPoGrnDto {

    /** Display PO number, e.g. {@code "PO-2025-074"}. */
    private String poNo;

    /** Proxy for "PO date" — uses {@code estimatedDeliveryDate} pending a real created-at column. */
    private LocalDate poDate;

    /** GRN display id, e.g. {@code "GRN-2025-088"}, or {@code "—"} when no GRN exists. */
    private String grnNo;

    /** GRN received date, or {@code null} when no GRN exists yet. */
    private LocalDate grnDate;

    /** First raw-material name on the PO, with a {@code "+N more"} suffix when extra items exist. */
    private String productSummary;

    /** Total received quantity on the GRN, falling back to PO required qty. */
    private BigDecimal qty;

    /** Unit of measure for the first line item. */
    private String unit;

    /** Amount: GRN total when received, otherwise PO total cost. */
    private BigDecimal amount;

    /** {@code "Delivered"} when a GRN exists, otherwise {@code "Pending"}. */
    private String status;
}
