package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single bucket on the dual-line / grouped-bar trend chart for FR-MIS-03.
 *
 * <p>The label format depends on the requested granularity:</p>
 * <ul>
 *   <li>{@code DAILY}   — {@code "MMM dd"} (e.g. {@code "Aug 23"})</li>
 *   <li>{@code MONTHLY} — month abbrev (e.g. {@code "Jan"})</li>
 *   <li>{@code YEARLY}  — year (e.g. {@code "2025"})</li>
 * </ul>
 *
 * <p>{@code lastYear} is the same bucket shifted back by exactly one year
 * (same calendar date for DAILY, same month for MONTHLY, previous year for
 * YEARLY). Buckets without prior-year data report {@code 0} so the line
 * stays continuous.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrendPointDto {

    /** Short label rendered on the X-axis. */
    private String label;

    /** Total spend in the bucket for the current year (or filter window). */
    private BigDecimal thisYear;

    /** Total spend in the same bucket one year earlier. */
    private BigDecimal lastYear;
}
