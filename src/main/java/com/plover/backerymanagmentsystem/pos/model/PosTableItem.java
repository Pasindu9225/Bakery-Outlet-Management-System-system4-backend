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

@Entity
@Table(name = "table_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosTableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "day_production_item_id", nullable = false)
    private Integer dayProductionItemId;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;

    @Column(name = "kot_id")
    private Long kotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", insertable = false, updatable = false)
    private PosTable posTable;
}
