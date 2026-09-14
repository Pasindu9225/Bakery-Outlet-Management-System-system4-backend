package com.plover.backerymanagmentsystem.finance.service;

import java.time.LocalDate;
import java.util.List;

import com.plover.backerymanagmentsystem.finance.dto.LedgerEntryDto;

/**
 * Service for assembling a supplier's full ledger from GRNs, POs, payments,
 * returns and manual adjustments and applying optional filters.
 */
public interface SupplierLedgerService {

    /**
     * Build the unified ledger for a supplier with optional filters.
     *
     * @param supplierId the supplier id (must not be {@code null})
     * @param from       start date (inclusive), nullable
     * @param to         end date (inclusive), nullable
     * @param type       filter by type ({@code GRN}/{@code PO}/{@code Payment}/{@code Return}/{@code Adjustment}), nullable
     * @param status     filter by status ({@code Pending}/{@code Cleared}/{@code Overdue}), nullable
     * @return ledger entries sorted by date descending; running balance is computed against the unfiltered series
     */
    List<LedgerEntryDto> getLedger(Long supplierId, LocalDate from, LocalDate to, String type, String status);
}
