package com.plover.backerymanagmentsystem.pos.model;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pos_waiter_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosWaiterItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "waiter_id", nullable = false, length = 16)
    private byte[] waiterId; // References BmsAuth.id

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

    @Column(name = "bill_id", length = 50)
    private String billId;
}
