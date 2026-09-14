package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "production_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_plan_id", nullable = true)
    private ProductionPlan productionPlan;
    
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;
    
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
    
    @Column(name = "planned_start_date")
    private LocalDateTime plannedStartDate;
    
    @Column(name = "planned_end_date")
    private LocalDateTime plannedEndDate;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductionOrderStatus status;
    
    @Column(name = "total_raw_material_cost")
    private Double totalRawMaterialCost;
    
    @Column(name = "total_production_cost")
    private Double totalProductionCost;
    
    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductionOrderItem> productionOrderItems;
    
    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RawMaterialRequirement> rawMaterialRequirements;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ProductionOrderStatus {
        PENDING, APPROVED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
