package com.plover.backerymanagmentsystem.finance.service;

import java.time.LocalDate;

import com.plover.backerymanagmentsystem.finance.dto.ReportEnvelopeDto;

/**
 * Service for FR-FIN-04 financial reports. Each report family returns a
 * {@link ReportEnvelopeDto} so the frontend can render uniformly across
 * categories.
 *
 * <p>Reports whose underlying data does not exist yet (profitability,
 * variance) deliberately return an envelope with {@code dataUnavailable=true}
 * and an empty rows list rather than throwing an error. This keeps the UI
 * graceful while the missing tables/columns are added in future slices.</p>
 */
public interface FinancialReportService {

    /**
     * Payment & outstanding summary report. Combines supplier payments with
     * the corresponding GRN totals so each row represents a payment-shaped
     * summary entry.
     *
     * @param period {@code Daily} / {@code Monthly} / {@code Yearly}; treated
     *               purely as a metadata label — caller decides the date range
     * @param from   inclusive start (nullable)
     * @param to     inclusive end (nullable)
     */
    ReportEnvelopeDto getPaymentSummary(String period, LocalDate from, LocalDate to);

    /**
     * Sales report. {@code type} controls the projection:
     * <ul>
     *   <li>{@code byProduct}: aggregate per product</li>
     *   <li>{@code byInterval}: aggregate per day</li>
     *   <li>{@code discountsReturns}: only rows with discount or return &gt; 0</li>
     * </ul>
     */
    ReportEnvelopeDto getSalesReport(String type, LocalDate from, LocalDate to, Long outletId);

    /**
     * Wastage / production-cost report. {@code type}:
     * <ul>
     *   <li>{@code byProduct}: wastage rolled up per product</li>
     *   <li>{@code byOutlet}: wastage rolled up per production center</li>
     *   <li>{@code rawMaterialCosts}: raw material costs from GRN (uses
     *       {@code from}/{@code to} as the GRN date range)</li>
     * </ul>
     */
    ReportEnvelopeDto getWastageReport(String type, LocalDate from, LocalDate to, Long outletId);

    /**
     * Profitability report. Currently returns {@code dataUnavailable=true}
     * because product cost / recipe-derived cost is not tracked yet.
     */
    ReportEnvelopeDto getProfitabilityReport(String type, LocalDate from, LocalDate to);

    /**
     * Staff meals report. Returns sales whose {@code paymentType=FREE_MEAL}.
     */
    ReportEnvelopeDto getStaffMealReport(LocalDate from, LocalDate to);

    /**
     * Stock movement report. {@code type}:
     * <ul>
     *   <li>{@code inflow}: GRN-based inflow</li>
     *   <li>{@code outflow}: returns and adjustments. Currently returns
     *       {@code dataUnavailable=true} for outflow because material issuance
     *       has no per-day rollup query in this slice.</li>
     *   <li>{@code adjustments}: stock adjustment log</li>
     *   <li>{@code currentStock}: current raw material stock levels</li>
     * </ul>
     */
    ReportEnvelopeDto getStockMovementReport(String type, LocalDate from, LocalDate to);

    /**
     * Purchase price analysis. Builds rows from GRN items and the change vs
     * the previous price for the same supplier+material.
     *
     * <p>{@code type} is one of {@code bySupplier} / {@code byProduct} /
     * {@code priceChange}; all three return the same data, sorted differently.</p>
     */
    ReportEnvelopeDto getPurchasePriceReport(String type, LocalDate from, LocalDate to);

    /**
     * Standard vs actual variance. Always {@code dataUnavailable=true} until
     * standard cost / quantity baselines are tracked.
     */
    ReportEnvelopeDto getVarianceReport(String type, LocalDate from, LocalDate to);
}
