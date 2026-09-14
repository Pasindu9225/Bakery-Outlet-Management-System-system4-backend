package com.plover.backerymanagmentsystem.mis.service;

import java.time.LocalDate;
import java.util.List;

import com.plover.backerymanagmentsystem.mis.dto.MisSupplierDetailDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierListItemDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierPoGrnDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierSummaryDto;
import com.plover.backerymanagmentsystem.mis.dto.MisSupplierTransactionDto;

/**
 * Service for the FR-MIS-02 Supplier Overview screen.
 *
 * <p>Combines supplier master data with the financial rollups produced by
 * the existing finance module — {@code FinanceSupplierService},
 * {@code OutstandingSummaryService}, and {@code SupplierLedgerService} — so
 * the MIS dashboard can render the supplier list, the four summary metric
 * cards, and the per-supplier detail modal in a single set of REST calls.</p>
 */
public interface MisSupplierService {

    /**
     * Compute the four supplier metrics surfaced at the top of the overview
     * page (total suppliers, active suppliers, aggregate outstanding,
     * overdue accounts).
     */
    MisSupplierSummaryDto getSummary();

    /**
     * Return the supplier table rows. All filters are combined with AND
     * server-side; {@code null}/{@code "All"} disables the filter.
     *
     * @param search    optional case-insensitive substring matched against
     *                  name, display id ({@code SUP-NNN}) and email
     * @param status    one of {@code "Active"}, {@code "Inactive"} or
     *                  {@code "All"} (case-insensitive); rejects unknown
     *                  values via {@link IllegalArgumentException}
     * @param startDate inclusive lower bound on {@code lastTransactionDate}
     * @param endDate   inclusive upper bound on {@code lastTransactionDate}
     * @return supplier rows, sorted by name ascending
     * @throws IllegalArgumentException if {@code startDate} is after {@code endDate}
     *                                  or {@code status} is not one of the allowed values
     */
    List<MisSupplierListItemDto> getSuppliers(String search, String status,
                                              LocalDate startDate, LocalDate endDate);

    /**
     * Return the detail-modal payload for a single supplier.
     *
     * @param supplierId the supplier id (must not be {@code null})
     * @return the supplier's profile + counts
     * @throws org.springframework.web.server.ResponseStatusException with
     *         {@link org.springframework.http.HttpStatus#NOT_FOUND} when the
     *         supplier does not exist
     */
    MisSupplierDetailDto getSupplierDetail(Long supplierId);

    /**
     * Return the supplier's full ledger as MIS transaction rows. Delegates
     * to the finance {@code SupplierLedgerService} and applies an optional
     * client-side text filter on {@code ref} / {@code description}.
     *
     * @param supplierId the supplier id (must not be {@code null})
     * @param search     optional case-insensitive substring match on
     *                   {@code ref} or {@code description}
     * @return ledger rows, newest first
     * @throws org.springframework.web.server.ResponseStatusException with
     *         {@link org.springframework.http.HttpStatus#NOT_FOUND} when the
     *         supplier does not exist
     */
    List<MisSupplierTransactionDto> getSupplierTransactions(Long supplierId, String search);

    /**
     * Return the joined PO/GRN listing for the supplier-detail modal.
     *
     * @param supplierId the supplier id (must not be {@code null})
     * @return one row per PO; rows are sorted by PO date descending
     * @throws org.springframework.web.server.ResponseStatusException with
     *         {@link org.springframework.http.HttpStatus#NOT_FOUND} when the
     *         supplier does not exist
     */
    List<MisSupplierPoGrnDto> getSupplierPoGrn(Long supplierId);
}
