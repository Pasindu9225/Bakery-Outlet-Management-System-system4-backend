package com.plover.backerymanagmentsystem.manager.model;

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
 * Entity representing a Purchase Order Item in the system.
 */
@Entity
@Table(name = "purchase_order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poi_id")
    private Long poiId;

    @Column(name = "raw_material_id", nullable = false)
    private Integer rawMaterialId;

    @Column(name = "required_qty", nullable = false)
    private Integer requiredQty;

    @Column(name = "received_qty")
    private Integer receivedQty;

    @Column(name = "actual_cost", precision = 12, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", referencedColumnName = "id", insertable = false, updatable = false)
    private RawMaterial rawMaterial;
}
