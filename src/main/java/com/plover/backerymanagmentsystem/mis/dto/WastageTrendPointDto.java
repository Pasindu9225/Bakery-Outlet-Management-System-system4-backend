package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single {label, value} point on the wastage-trend line chart. The value
 * is the daily wastage value (or quantity when value is unavailable).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WastageTrendPointDto {

    /** ISO-8601 date for the bucket. */
    private LocalDate date;

    /** Short human-friendly label suitable for the X-axis (e.g. "Apr 30"). */
    private String label;

    /** Wastage value for the day. May be {@link BigDecimal#ZERO} for
     *  empty days within the window so the line stays continuous. */
    private BigDecimal value;

    /** Total quantity for the day — useful when value is unavailable. */
    private long qty;
}
