package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Drill-down payload for FR-FIN-02. Wraps the same per-supplier rollup fields
 * as {@link SupplierOutstandingDto} together with the supplier's full unpaid
 * GRN list (re-using the existing {@link OutstandingGrnDto}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierOutstandingDetailDto {

    private String id;
    private Long supplierId;
    private String name;
    private String type;

    private BigDecimal totalOutstanding;
    private BigDecimal overdueAmount;
    private int unpaidInvoices;
    private LocalDate earliestDueDate;
    private LocalDate lastTransaction;

    private String phone;
    private String email;
    private String address;

    /** Full list of GRNs with an outstanding balance for this supplier. */
    private List<OutstandingGrnDto> invoices;
}
