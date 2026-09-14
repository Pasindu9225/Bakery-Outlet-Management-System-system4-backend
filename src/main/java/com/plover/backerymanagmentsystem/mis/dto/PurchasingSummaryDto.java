package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-of-page metric envelope for the FR-MIS-03 purchasing-trends dashboard.
 *
 * <p>Drives the four summary cards (Total Purchase Orders, Total Spend,
 * Average Order Value, Pending POs).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchasingSummaryDto {

    /** Number of POs in the filtered window. */
    private long totalPos;

    /** Sum of {@code totalCost} over the filtered POs. */
    private BigDecimal totalSpend;

    /** {@code totalSpend / totalPos}, rounded to 2dp. {@code 0} when
     *  {@code totalPos == 0}. */
    private BigDecimal avgOrderValue;

    /** POs that have not yet been fully received (no GRN, or GRN
     *  {@code grnStatus} other than {@code RECEIVED}). */
    private long pendingPos;
}
