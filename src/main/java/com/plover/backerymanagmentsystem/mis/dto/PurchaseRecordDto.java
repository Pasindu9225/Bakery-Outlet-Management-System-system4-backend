package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single PO drill-down row exposed by the FR-MIS-03 purchasing-trends
 * dashboard ({@code GET /api/v1/mis/purchasing/dashboard} and
 * {@code /records}).
 *
 * <p>One row per {@code PurchaseOrderItem} so the table can show category /
 * product breakdowns within the same PO. {@code grnNo} is {@code "—"} when
 * the PO has not been received yet; {@code status} is derived from the
 * presence of a linked GRN ({@code "Received"} when a GRN row exists with
 * {@code grnStatus=RECEIVED}, {@code "Partial"} when {@code PARTIAL},
 * otherwise {@code "Pending"}).</p>
 *
 * <p>{@code poDate} is sourced from {@link com.plover.backerymanagmentsystem.manager.model.PurchaseOrder#getEstimatedDeliveryDate()}
 * because the entity has no {@code created_at} column today — see service
 * Javadoc for the limitation.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseRecordDto {

    /** Display PO number, e.g. {@code "PO-2025-074"}. */
    private String poNo;

    /** Raw PO id — useful for click-through filtering. */
    private Long poId;

    /** Proxy "PO date" sourced from {@code estimatedDeliveryDate}. */
    private LocalDate poDate;

    /** Supplier display name. */
    private String supplier;

    /** Raw supplier id — null when the supplier record could not be resolved. */
    private Long supplierId;

    /** Material category (from {@code GenericMaterial.category}); falls back
     *  to {@code "Uncategorized"} when no category is recorded. */
    private String category;

    /** Outlet display name — currently always {@code "Central Store"}
     *  because POs are placed centrally rather than per-outlet. */
    private String outlet;

    /** Outlet id — always {@code null} because POs are not per-outlet. */
    private Long outletId;

    /** Raw-material name (one per row). */
    private String product;

    /** Raw-material id. */
    private Long productId;

    /** Quantity ordered or received (received qty when available). */
    private BigDecimal qty;

    /** Unit of measure (e.g. {@code "kg"}, {@code "L"}). */
    private String unit;

    /** Per-unit cost. */
    private BigDecimal unitCost;

    /** Line total ({@code qty * unitCost}). */
    private BigDecimal totalCost;

    /** GRN display number, e.g. {@code "GRN-2025-088"}; {@code "—"} when
     *  the PO is not yet received. */
    private String grnNo;

    /** Display status — one of {@code "Received"} / {@code "Partial"} /
     *  {@code "Pending"}. */
    private String status;
}
