package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "distributhio_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dp_id")
    private Long dpId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_plan_id")
    private ProductionPlan productionPlan;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "status")
    @Convert(converter = DistributionPlanStatusConverter.class)
    private DistributionPlanStatus status;
    
    @OneToMany(mappedBy = "distributionPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DistributionPlanItem> distributionPlanItems;
}
