package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "production_order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "planned_quantity", nullable = false)
    private Integer plannedQuantity;
    
    @Column(name = "completed_quantity")
    private Integer completedQuantity;
    
    @Column(name = "unit_cost")
    private Double unitCost;
    
    @Column(name = "total_cost")
    private Double totalCost;
    
    @Column(name = "raw_material_cost")
    private Double rawMaterialCost;

    @Column(name = "production_center_id")
    private Long productionCenterId;
}
