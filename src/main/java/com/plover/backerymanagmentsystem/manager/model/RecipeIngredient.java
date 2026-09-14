package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recipe_ingredients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", nullable = false)
    private RawMaterial rawMaterial;
    
    @Column(name = "quantity_per_unit", nullable = false)
    private Double quantityPerUnit;
    
    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;
    
    @Column(name = "cost_per_unit")
    private Double costPerUnit;
    
    @Column(name = "total_cost_per_unit")
    private Double totalCostPerUnit;
}
