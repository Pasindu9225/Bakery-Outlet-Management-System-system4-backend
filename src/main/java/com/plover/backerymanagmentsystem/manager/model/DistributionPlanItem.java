package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "distribution_plan_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionPlanItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dpi_id")
    private Long dpiId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "qty", nullable = false)
    private Integer qty;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dp_id", nullable = false)
    private DistributionPlan distributionPlan;
}
