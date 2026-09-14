package com.plover.backerymanagmentsystem.finance.service;

import java.time.LocalDate;
import java.util.List;

import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDetailDto;
import com.plover.backerymanagmentsystem.finance.dto.SupplierOutstandingDto;

/**
 * Service for the FR-FIN-02 supplier outstanding-summary screen. Computes a
 * per-supplier rollup of unpaid GRN balances and exposes a drill-down for the
 * full unpaid invoice list per supplier.
 */
public interface OutstandingSummaryService {

    /**
     * Build the outstanding-summary list across all suppliers. Suppliers with
     * a zero {@code totalOutstanding} are excluded by default unless
     * {@code includeAll=true}.
     *
     * <p>All filters are combined with AND.</p>
     *
     * @param search       optional case-insensitive substring matched against
     *                     supplier name and supplier display id ({@code SUP-X})
     * @param overdueOnly  when {@code true}, return only suppliers with a
     *                     positive {@code overdueAmount}
     * @param dueFrom      optional inclusive lower bound on
     *                     {@code earliestDueDate}
     * @param dueTo        optional inclusive upper bound on
     *                     {@code earliestDueDate}
     * @param includeAll   when {@code true}, do not skip suppliers with a zero
     *                     total outstanding
     * @return the per-supplier outstanding rollup, sorted by
     *         {@code totalOutstanding} descending
     */
    List<SupplierOutstandingDto> getSummary(String search, Boolean overdueOnly,
                                            LocalDate dueFrom, LocalDate dueTo,
                                            Boolean includeAll);

    /**
     * Drill-down for a single supplier: returns the same rollup fields plus
     * the full list of GRNs with an outstanding balance (re-using the existing
     * {@code OutstandingGrnDto} projection from FR-FIN-03).
     *
     * @param supplierId the supplier id (must not be {@code null})
     * @return the supplier's drill-down payload
     * @throws IllegalArgumentException if no such supplier exists
     */
    SupplierOutstandingDetailDto getDetail(Long supplierId);
}
