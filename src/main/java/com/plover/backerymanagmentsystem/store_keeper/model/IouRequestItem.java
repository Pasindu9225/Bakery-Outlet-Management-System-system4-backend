package com.plover.backerymanagmentsystem.store_keeper.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "iou_request_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IouRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iou_request_id", nullable = false)
    private IouRequest iouRequest;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "supplier_contact")
    private String supplierContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private IouItemType itemType;

    @Column(name = "raw_material_id")
    private Long rawMaterialId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "estimated_quantity")
    private Double estimatedQuantity;

    @Column(name = "estimated_price")
    private Double estimatedPrice;

    @Column(name = "actual_quantity")
    private Double actualQuantity;

    @Column(name = "actual_price")
    private Double actualPrice;

    @Column(name = "actual_item_name")
    private String actualItemName;

    @Column(name = "actual_supplier_name")
    private String actualSupplierName;
}
