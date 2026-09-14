package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single drill-down row in the wastage dashboard (one wastage event from
 * any source: production batch, POS return, outlet return, or stock
 * adjustment).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WastageRecordDto {

    /** Synthetic record id with a source-specific prefix. */
    private String id;

    /** ISO-8601 date the wastage was recorded. */
    private LocalDate date;

    /** Display name of the outlet / production center. */
    private String outlet;

    /** Outlet id used for filtering — may be null for stock adjustments
     *  which live at the warehouse level. */
    private Long outletId;

    /** Display name of the product / raw material. */
    private String product;

    /** Product id used for filtering — null when wastage is for raw
     *  material rather than a finished product. */
    private Long productId;

    /** Wastage quantity. */
    private Integer qty;

    /** Unit of measure ("units", "kg", etc.). */
    private String unit;

    /** Estimated value of the wastage. May be null when no unit price is
     *  available; the UI shows "—" in that case. */
    private BigDecimal value;

    /** Canonical reason: EXPIRED / DAMAGED / RETURNED / OTHER. */
    private String reason;

    /** Original raw reason captured by the source (free-text or enum
     *  display name) — kept verbatim for the drill-down. */
    private String rawReason;

    /** Free-text remark / context (e.g. batch notes). */
    private String remarks;

    /** Source module: PRODUCTION / POS_RETURN / OUTLET_RETURN /
     *  STOCK_ADJUSTMENT. */
    private String source;
}
