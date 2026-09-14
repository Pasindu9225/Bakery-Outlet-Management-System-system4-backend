package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-of-page metric envelope for the FR-MIS-02 supplier overview.
 *
 * <p>Drives the four summary cards (Total Suppliers, Active Suppliers,
 * Total Outstanding, Overdue Accounts).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisSupplierSummaryDto {

    private long totalSuppliers;

    private long activeCount;

    private BigDecimal totalOutstanding;

    private long overdueCount;
}
