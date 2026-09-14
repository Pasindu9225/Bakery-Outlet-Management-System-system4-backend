package com.plover.backerymanagmentsystem.store_keeper.model;

import java.math.BigDecimal;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;

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
 * Entity representing an individual item in a Goods Receipt Note (GRN). Each
 * GRN item corresponds to a specific raw material in the GRN.
 */
@Entity
@Table(name = "grn_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grn_item_id")
    private Long grnItemId;

    @Column(name = "received_quantity", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @Column(name = "uom", nullable = false, length = 10)
    private String uom;

    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pricePerUnit = BigDecimal.ZERO;

    @Column(name = "invoice_quantity", precision = 12, scale = 3)
    private BigDecimal invoiceQuantity;

    @Column(name = "invoice_price", precision = 12, scale = 2)
    private BigDecimal invoicePrice;

    @Column(name = "raw_material_id", nullable = false)
    private Long rawMaterialId;

    @Column(name = "grn_id", nullable = false)
    private Long grnId;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "expire_date")
    private java.time.LocalDate expireDate;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", referencedColumnName = "id", insertable = false, updatable = false)
    private RawMaterial rawMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", referencedColumnName = "grn_id", insertable = false, updatable = false)
    private Grn grn;
}
