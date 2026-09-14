package com.plover.backerymanagmentsystem.mis.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envelope returned by {@code GET /api/v1/mis/wastage/dashboard}. Bundles
 * the four widgets (summary cards, top-products bar, outlet hotspot list,
 * reason pie, daily trend, drill-down records) into a single payload so the
 * frontend can render the entire dashboard in one round-trip.
 *
 * <p>{@code dataLimitation} is set to a short token (e.g.
 * {@code "value-not-available"}) when an upstream source did not surface
 * a unit price/value. The UI uses this to show a footnote rather than
 * crashing or mis-summing.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WastageDashboardDto {

    private LocalDate from;
    private LocalDate to;

    private WastageSummaryDto summary;

    /** Top wasted products by value (descending). */
    @Builder.Default
    private List<WastageGroupDto> topProducts = List.of();

    /** Outlet / production-center hotspots. */
    @Builder.Default
    private List<WastageGroupDto> outletBreakdown = List.of();

    /** Reason distribution — used for the pie chart. */
    @Builder.Default
    private List<WastageGroupDto> reasonBreakdown = List.of();

    /** Daily trend bucketed across the window. */
    @Builder.Default
    private List<WastageTrendPointDto> trend = List.of();

    /** Drill-down rows. Capped on the server to keep payload small;
     *  callers needing more should use {@code /records}. */
    @Builder.Default
    private List<WastageRecordDto> records = List.of();

    /** Optional limitation tag set when upstream data is partial. */
    private String dataLimitation;

    /** Optional human-readable explanation of the limitation. */
    private String dataLimitationReason;
}
