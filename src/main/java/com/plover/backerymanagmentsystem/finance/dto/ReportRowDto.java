package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single row in a financial report. Uses a single shared shape so the
 * frontend can render any report with a predictable structure. Fields that
 * don't apply to a given report category are simply left {@code null} and
 * omitted from the JSON output via {@link JsonInclude.Include#NON_NULL}.
 *
 * <p>Field meanings by report category:</p>
 * <ul>
 *   <li><b>Payments</b>: id, date, supplier, ref, totalInvoiced, paid,
 *       outstanding, status</li>
 *   <li><b>Sales</b>: id, date, product, category, outlet, sales, discounts,
 *       returns, net</li>
 *   <li><b>Wastage</b>: id, date, product, category, outlet, qty, unit,
 *       costPerUnit, totalCost, reason</li>
 *   <li><b>Staff Meals</b>: id, date, employee (uses supplier slot for
 *       UI friendliness — see field doc), product, totalCost, reason</li>
 *   <li><b>Stock Movement</b>: id, date, product, supplier, qty, unit,
 *       costPerUnit, totalCost, reason (movement type)</li>
 *   <li><b>Purchase Price</b>: id, date, supplier, product, costPerUnit,
 *       qty, totalCost, change (vs previous)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportRowDto {

    /** Stable id for keying rows in the UI (e.g. {@code "PMT-12"}). */
    private String id;

    /** ISO date string, e.g. {@code "2026-04-30"}. */
    private String date;

    // ── Payments / staff-meal / stock-movement supplier slot ──────
    private String supplier;
    private String ref;
    private BigDecimal totalInvoiced;
    private BigDecimal paid;
    private BigDecimal outstanding;
    private String status;

    // ── Sales / wastage / staff-meal / stock-movement product ─────
    private String product;
    private String category;
    private String outlet;
    private BigDecimal sales;
    private BigDecimal discounts;

    /**
     * Returns total — note this clashes with the Java keyword {@code returns}
     * so callers must use the getter/setter directly.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("returns")
    private BigDecimal returnsAmount;

    private BigDecimal net;
    private Integer qty;
    private String unit;
    private BigDecimal costPerUnit;
    private BigDecimal totalCost;
    private String reason;

    // ── Purchase-price specific: % change vs previous price ──────
    private BigDecimal change;

    // ── Staff-meal specific: cashier display name ────────────────
    private String employee;
}
