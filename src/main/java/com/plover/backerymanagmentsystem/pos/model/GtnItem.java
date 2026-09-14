package com.plover.backerymanagmentsystem.pos.model;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.manager.model.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an item in a GTN (Goods Transfer Note). Each item
 * represents a specific product with quantities and details.
 */
@Entity
@Table(name = "gtn_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GtnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gtn_item_id")
    private Integer gtnItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gtn_id", nullable = false)
    private Gtn gtn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "expected_qty", nullable = false)
    private Double expectedQty;

    @Column(name = "received_qty", nullable = false, columnDefinition = "DOUBLE DEFAULT 0")
    private Double receivedQty;

    @Column(name = "status", nullable = false)
    @Convert(converter = com.plover.backerymanagmentsystem.pos.model.converter.GtnStatusConverter.class)
    private GtnStatus status;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "unit", nullable = false)
    @Convert(converter = com.plover.backerymanagmentsystem.pos.model.converter.ProductUnitConverter.class)
    private ProductUnit unit;

    @Column(name = "entry_status", nullable = false)
    @Convert(converter = com.plover.backerymanagmentsystem.pos.model.converter.EntryStatusConverter.class)
    private EntryStatus entryStatus;

    @Column(name = "remarks")
    private String remarks;
}
