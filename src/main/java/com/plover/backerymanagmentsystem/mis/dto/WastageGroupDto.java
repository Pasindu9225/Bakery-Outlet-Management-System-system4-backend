package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic {label, qty, value} bucket used for product / outlet / reason
 * groupings rendered as bar / pie / hotspot widgets on the dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WastageGroupDto {

    /** Display label (product name, outlet name, or reason bucket). */
    private String label;

    /** Group key that pairs with {@link #label}; e.g. product/outlet id
     *  or canonical reason code. Useful for click-through filters. */
    private String key;

    /** Total wasted quantity in the bucket. */
    private long qty;

    /** Total wasted value in the bucket. {@code null} when no records
     *  contributed a value. */
    private BigDecimal value;

    /** Number of underlying wastage records in the bucket. */
    private int recordCount;
}
