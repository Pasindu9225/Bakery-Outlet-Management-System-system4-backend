package com.plover.backerymanagmentsystem.pos.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.plover.backerymanagmentsystem.pos.model.EntryStatus;
import com.plover.backerymanagmentsystem.pos.model.GtnSource;
import com.plover.backerymanagmentsystem.pos.model.GtnStatus;
import com.plover.backerymanagmentsystem.pos.model.ProductUnit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new GTN (Goods Transfer Note) with its items.
 * This is a temporary endpoint used to seed data into the GTN tables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGtnRequestDto {

    // ── GTN header fields ──────────────────────────────────────────────────────

    /**
     * Date/time of the GTN. If null, the current date/time will be used.
     */
    private LocalDateTime date;

    /**
     * Source of the GTN. Accepted values: BACKERY, STORE, KITCHEN, UNDEFINED
     */
    private GtnSource source;

    /**
     * UUID string of the user who is adding this GTN (addedBy).
     * Must be a valid UUID format, e.g. "550e8400-e29b-41d4-a716-446655440000"
     */
    private String addedBy;

    /**
     * UUID string of the user who approved this GTN (approvedBy). Optional.
     */
    private String approvedBy;

    /**
     * Outlet ID representing the destination of the goods. Optional.
     */
    private Long outletId;

    /**
     * Overall GTN status.
     * Accepted values: NOT_RECEIVED, PARTIALLY_RECEIVED, RECEIVED, OVER_RECEIVED
     */
    private GtnStatus status;

    // ── GTN items ─────────────────────────────────────────────────────────────

    private List<GtnItemRequest> items;

    // ── Inner class: per-line item ────────────────────────────────────────────

    /**
     * Represents a single product line inside the GTN.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GtnItemRequest {

        /**
         * ID of the product (references products.id)
         */
        private Long productId;

        /**
         * Expected quantity to be received
         */
        private Double expectedQty;

        /**
         * Already-received quantity. Defaults to 0 if omitted.
         */
        private Double receivedQty;

        /**
         * Item-level status string stored in gtn_item.status.
         * Accepted values: NOT_RECEIVED, PARTIALLY_RECEIVED, RECEIVED, OVER_RECEIVED
         */
        private GtnStatus status;

        /**
         * Expiry date for this product batch. Required.
         */
        private LocalDateTime expiryDate;

        /**
         * Unit of measurement.
         * Accepted values: PIECES, KG, LITERS, BOXES, PACKETS
         */
        private ProductUnit unit;

        /**
         * Entry method for this item.
         * Accepted values: MANUAL, SYSTEM
         */
        private EntryStatus entryStatus;

        /**
         * Optional remarks / notes for this item
         */
        private String remarks;
    }
}
