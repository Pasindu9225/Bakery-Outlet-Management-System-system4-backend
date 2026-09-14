package com.plover.backerymanagmentsystem.mis.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envelope returned by {@code GET /api/v1/mis/purchasing/dashboard}. Bundles
 * the four dashboard widgets (summary cards, dual-line trend, category
 * donut, supplier ranking) plus the drill-down records into a single payload
 * so the page renders in one round-trip.
 *
 * <p>{@code dataLimitation} is set to a short token (e.g.
 * {@code "outlet-filter-noop"}) when an upstream constraint forced a filter
 * to be ignored. The UI shows a footnote rather than crashing or
 * mis-summing.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchasingDashboardDto {

    private LocalDate from;
    private LocalDate to;

    /** Bucket granularity (DAILY / MONTHLY / YEARLY) that produced the trend series. */
    private String granularity;

    /** Four summary cards. */
    private PurchasingSummaryDto summary;

    /** Dual-line / grouped-bar series — this year vs last year. */
    @Builder.Default
    private List<TrendPointDto> trend = List.of();

    /** Category-spend slices for the donut chart. */
    @Builder.Default
    private List<CategoryBreakdownDto> categoryBreakdown = List.of();

    /** Supplier spend ranking (sorted desc by {@code totalSpend}). */
    @Builder.Default
    private List<SupplierSpendDto> supplierRanking = List.of();

    /** Drill-down rows — capped on the server so the envelope stays small.
     *  Callers needing the full set should hit {@code /records}. */
    @Builder.Default
    private List<PurchaseRecordDto> records = List.of();

    /** Optional limitation tag (currently {@code "outlet-filter-noop"} or
     *  {@code "category-data-missing"}). */
    private String dataLimitation;

    /** Optional human-readable explanation of the limitation. */
    private String dataLimitationReason;
}
