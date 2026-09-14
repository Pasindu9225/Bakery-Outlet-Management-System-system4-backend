package com.plover.backerymanagmentsystem.pos.model;

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

@Entity
@Table(name = "day_end_closing_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayEndClosingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "closing_item_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closing_id", nullable = false)
    private DayEndClosing dayEndClosing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "system_qty", nullable = false)
    private Integer systemQty;

    @Column(name = "physical_qty", nullable = false)
    private Integer physicalQty;

    @Column(name = "carry_forward_qty", nullable = false)
    private Integer carryForwardQty;

    @Column(name = "wastage_qty", nullable = false)
    private Integer wastageQty;
}
