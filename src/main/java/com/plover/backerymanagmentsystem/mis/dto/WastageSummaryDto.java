package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate metrics powering the four summary cards on the wastage
 * dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WastageSummaryDto {

    /** Sum of wasted quantities across every record in the filter
     *  window. */
    private long totalQty;

    /** Sum of wasted values. {@code null} when no source provides a
     *  unit price/value (e.g. POS-return-only data without sale prices
     *  carried through). */
    private BigDecimal totalValue;

    /** Wastage value as a percentage of sales for the same period. Set to
     *  {@code null} when {@link #salesDataAvailable} is {@code false}. */
    private BigDecimal wastagePercent;

    /** Count of underlying wastage rows. */
    private int recordCount;

    /** {@code true} when a sales total was found for the same window so
     *  the percentage above is meaningful. */
    @Builder.Default
    private boolean salesDataAvailable = true;

    /** Sales-of-period denominator used to compute {@link #wastagePercent}.
     *  Surfaced so the UI can show "$X waste / $Y sales = Z%". */
    private BigDecimal salesValue;
}
