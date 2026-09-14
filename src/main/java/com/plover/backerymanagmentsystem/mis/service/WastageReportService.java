package com.plover.backerymanagmentsystem.mis.service;

import java.time.LocalDate;
import java.util.List;

import com.plover.backerymanagmentsystem.mis.dto.WastageDashboardDto;
import com.plover.backerymanagmentsystem.mis.dto.WastageRecordDto;

/**
 * Service powering FR-MIS-01 — the centralised wastage dashboard for MIS
 * Admins. Consolidates wastage from production batches, POS customer
 * returns, outlet→store returns, and stock adjustments into a single set
 * of metrics, groupings, trends, and drill-down records.
 */
public interface WastageReportService {

    /**
     * Build the full dashboard envelope for the supplied filters.
     *
     * @param startDate inclusive start (nullable; null = unbounded)
     * @param endDate   inclusive end (nullable; null = today). Reject any
     *                  endDate that is in the future.
     * @param outletId  optional outlet/production-center id filter
     * @param productId optional product id filter
     * @param reason    optional canonical reason (EXPIRED / DAMAGED /
     *                  RETURNED / OTHER); case-insensitive
     */
    WastageDashboardDto getDashboard(LocalDate startDate,
                                     LocalDate endDate,
                                     Long outletId,
                                     Long productId,
                                     String reason);

    /**
     * Return the drill-down list of wastage records for the supplied
     * filters. Pagination is applied server-side.
     *
     * @param page zero-based page index (defaults to 0)
     * @param size page size (defaults to 50, capped to 500)
     */
    List<WastageRecordDto> getRecords(LocalDate startDate,
                                      LocalDate endDate,
                                      Long outletId,
                                      Long productId,
                                      String reason,
                                      int page,
                                      int size);
}
