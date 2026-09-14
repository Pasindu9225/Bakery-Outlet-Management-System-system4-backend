package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Row in the FR-MIS-02 supplier overview table.
 *
 * <p>Combines {@link com.plover.backerymanagmentsystem.store_keeper.model.Supplier}
 * master data with the per-supplier financial rollup from FR-FIN-02. The
 * {@code status} field is computed (no DB column) from recent activity:
 * {@code Active} when there is a GRN or payment within the last 90 days,
 * {@code Inactive} otherwise.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisSupplierListItemDto {

    /** Display id, e.g. {@code "SUP-001"}. */
    private String id;

    /** Raw supplier id used for drill-down lookups. */
    private Long supplierId;

    private String name;

    /** Synonym for {@code phone}; populated from {@code Supplier.contactNumber}. */
    private String phone;

    private String email;

    private String address;

    /** Computed: {@code "Active"} or {@code "Inactive"}. */
    private String status;

    /** Aggregate unpaid GRN balance (less purchase-return credits), never negative. */
    private BigDecimal outstandingBalance;

    /** {@code true} when {@code overdueAmount > 0} for this supplier. */
    private boolean overdue;

    /** Latest of GRN.receivedDate, payment date or return date for this supplier. */
    private LocalDate lastTransactionDate;

    /** Total purchase orders ever raised against this supplier. */
    private long totalPos;

    /** Total GRNs (any status) ever recorded against this supplier. */
    private long totalGrns;

    /** Synthetic registration id, e.g. {@code "REG-2024-0042"}. */
    private String registrationId;

    /** ISO-8601 date derived from {@code Supplier.createdAt}; may be {@code null}. */
    private LocalDate registrationDate;
}
