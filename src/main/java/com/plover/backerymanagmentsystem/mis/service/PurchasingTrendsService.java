package com.plover.backerymanagmentsystem.mis.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.plover.backerymanagmentsystem.mis.dto.PurchaseRecordDto;
import com.plover.backerymanagmentsystem.mis.dto.PurchasingDashboardDto;
import com.plover.backerymanagmentsystem.mis.dto.TrendPointDto;

/**
 * Service powering FR-MIS-03 — the centralised purchasing-trends dashboard
 * for MIS Admins. Aggregates {@link com.plover.backerymanagmentsystem.manager.model.PurchaseOrder}
 * + {@link com.plover.backerymanagmentsystem.store_keeper.model.Grn}
 * data into trend buckets (daily / monthly / yearly), category and supplier
 * breakdowns, and drill-down records.
 */
public interface PurchasingTrendsService {

    /** Granularity options understood by the trend bucketing logic. */
    enum Granularity {
        DAILY, MONTHLY, YEARLY;

        /**
         * Parse a granularity string from the wire. Defaults to {@link #MONTHLY}
         * when {@code raw} is {@code null} / blank. Throws
         * {@link IllegalArgumentException} for unknown values so the controller
         * can translate to a {@code 400}.
         */
        public static Granularity from(String raw) {
            if (raw == null || raw.isBlank()) return MONTHLY;
            try {
                return Granularity.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "Invalid granularity '" + raw + "'. Allowed: DAILY, MONTHLY, YEARLY.");
            }
        }
    }

    /**
     * Build the full dashboard envelope for the supplied filters.
     *
     * @param startDate   inclusive start (nullable; null = unbounded)
     * @param endDate     inclusive end (nullable; null = today). Reject
     *                    any {@code endDate} that is in the future.
     * @param supplierId  optional supplier id filter
     * @param category    optional raw-material category filter
     *                    (case-insensitive); {@code "All"}/{@code null}
     *                    disables the filter
     * @param outletId    optional outlet filter — currently a no-op
     *                    because POs are placed centrally rather than
     *                    per-outlet (the response sets
     *                    {@code dataLimitation="outlet-filter-noop"}
     *                    when this is non-null)
     * @param granularity DAILY / MONTHLY / YEARLY (defaults to MONTHLY)
     */
    PurchasingDashboardDto getDashboard(LocalDate startDate,
                                        LocalDate endDate,
                                        Long supplierId,
                                        String category,
                                        Long outletId,
                                        Granularity granularity);

    /**
     * Build only the trend series — useful when the user changes the
     * granularity dropdown without needing the full dashboard.
     */
    List<TrendPointDto> getTrend(LocalDate startDate,
                                 LocalDate endDate,
                                 Long supplierId,
                                 String category,
                                 Long outletId,
                                 Granularity granularity);

    /**
     * Return the drill-down list of purchase records for the supplied
     * filters. Pagination is applied server-side using Spring's
     * {@link Page} envelope so the React table can render its pager.
     *
     * @param search optional case-insensitive substring matched against
     *               PO number, supplier name and product name
     */
    Page<PurchaseRecordDto> getRecords(LocalDate startDate,
                                       LocalDate endDate,
                                       Long supplierId,
                                       String category,
                                       Long outletId,
                                       String search,
                                       Pageable pageable);
}
