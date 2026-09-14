package com.plover.backerymanagmentsystem.pos.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity representing items associated with a return or exchange transaction
 */
@Entity
@Table(name = "return_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Integer returnItemId;

    @Column(name = "return_id", nullable = false)
    private Integer returnId;

    @Column(name = "sale_item_id", nullable = false)
    private Integer saleItemId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "is_resellable", nullable = false)
    private Boolean isResellable;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", referencedColumnName = "return_id", insertable = false, updatable = false)
    private Return returnRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_item_id", referencedColumnName = "sale_item_id", insertable = false, updatable = false)
    private SaleItem saleItem;
}
