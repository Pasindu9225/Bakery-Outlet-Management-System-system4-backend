package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envelope wrapping a list of {@link ReportRowDto} together with the report
 * metadata so the frontend can render headers, period labels and an
 * "unavailable" state without needing a separate API call.
 *
 * <p>{@code dataUnavailable} is set to {@code true} for reports whose source
 * data does not yet exist in the system (e.g. profitability requires unit
 * cost; variance requires standard cost/quantity baselines that are not
 * tracked yet). In those cases {@code rows} is an empty list and the UI
 * should render an informational empty state rather than an error.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportEnvelopeDto {

    /** e.g. {@code "payments"}, {@code "sales"}, {@code "wastage"}. */
    private String reportType;

    /** Sub-report kind (e.g. {@code "byProduct"}, {@code "byInterval"}). */
    private String subType;

    /** Aggregation period (e.g. {@code "Daily"}, {@code "Monthly"}). */
    private String period;

    private LocalDate from;
    private LocalDate to;

    /**
     * When {@code true} the row data is intentionally empty because the
     * underlying source data isn't tracked yet. The UI should display a
     * friendly empty state instead of a table.
     */
    @Builder.Default
    private boolean dataUnavailable = false;

    /** Optional human-readable explanation when {@code dataUnavailable}. */
    private String unavailableReason;

    @Builder.Default
    private List<ReportRowDto> rows = List.of();

    /** Optional summary aggregates keyed by metric name. */
    private Map<String, BigDecimal> totals;
}
