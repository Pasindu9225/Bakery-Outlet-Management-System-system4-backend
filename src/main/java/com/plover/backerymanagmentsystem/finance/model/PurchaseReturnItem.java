package com.plover.backerymanagmentsystem.finance.model;

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
 * Line item of a {@link PurchaseReturn} capturing the raw material returned,
 * the returned quantity and price.
 */
@Entity
@Table(name = "purchase_return_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Long returnItemId;

    @Column(name = "return_id", nullable = false, insertable = false, updatable = false)
    private Long returnId;

    @Column(name = "raw_material_id", nullable = false)
    private Long rawMaterialId;

    @Column(name = "returned_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal returnedQty;

    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(name = "reason", length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", referencedColumnName = "return_id", nullable = false)
    private PurchaseReturn purchaseReturn;
}
