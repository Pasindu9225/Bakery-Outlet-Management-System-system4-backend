package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "raw_material_requirements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialRequirement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false)
    private ProductionOrder productionOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", nullable = false)
    private RawMaterial rawMaterial;
    
    @Column(name = "required_quantity", nullable = false)
    private Double requiredQuantity;
    
    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;
    
    @Column(name = "unit_cost", nullable = false)
    private Double unitCost;
    
    @Column(name = "total_cost", nullable = false)
    private Double totalCost;
    
    @Column(name = "available_stock")
    private Double availableStock;
    
    @Column(name = "stock_deficit")
    private Double stockDeficit;
}
