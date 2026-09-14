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
@Table(name = "production_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "plan_name", nullable = false)
    private String planName;
    
    @Column(name = "plan_date", nullable = false)
    private LocalDateTime planDate;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProductionPlanStatus status;
    
    @Column(name = "total_estimated_cost")
    private Double totalEstimatedCost;
    
    @Column(name = "notes")
    private String notes;

    @Column(name = "department")
    private String department;

    @Column(name = "is_template")
    @Builder.Default
    private Boolean isTemplate = false;
    
    @OneToMany(mappedBy = "productionPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductionPlanItem> productionPlanItems;

    @OneToMany(mappedBy = "productionPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DistributionPlan> distributionPlans;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ProductionPlanStatus {
        DRAFT, SUBMITTED, APPROVED, IN_PROGRESS, COMPLETED, DISTRIBUTED, CANCELLED
    }
}
