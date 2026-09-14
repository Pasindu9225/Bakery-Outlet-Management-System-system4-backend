package com.plover.backerymanagmentsystem.pos.model;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.manager.model.Product;

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
 * Entity representing items in daily production plan.
 */
@Entity
@Table(name = "day_production_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayProductionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "day_production_item_id")
    private Integer dayProductionItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id", nullable = false)
    private DayProduction dayProduction;

    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty;

    @Column(name = "received_qty")
    private Integer receivedQty;

    @Column(name = "current_qty")
    private Integer currentQty;

    @Column(name = "wastage_qty")
    private Integer wastageQty;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
