package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "production_plan_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_plan_id", nullable = false)
    private ProductionPlan productionPlan;
    
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "raw_material_id")
    private Long rawMaterialId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_cost")
    private Double unitCost;
    
    @Column(name = "total_cost")
    private Double totalCost;
    
    @Column(name = "estimated_raw_material_cost")
    private Double estimatedRawMaterialCost;

    @Column(name = "production_center_id")
    private Long productionCenterId;

    @Column(name = "is_top_level")
    @Builder.Default
    private Boolean isTopLevel = false;

    @Column(name = "parent_plan_item_id")
    private Long parentPlanItemId;

    @Column(name = "reserved_from_mini_store")
    @Builder.Default
    private Double reservedFromMiniStore = 0.0;

    @Column(name = "mini_store_fulfilled")
    @Builder.Default
    private Boolean miniStoreFulfilled = false;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "exact_quantity")
    private Double exactQuantity;
}
