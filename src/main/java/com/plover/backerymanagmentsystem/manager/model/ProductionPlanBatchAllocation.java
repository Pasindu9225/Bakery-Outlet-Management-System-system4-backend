package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_plan_batch_allocations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanBatchAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_plan_id", nullable = false)
    private Long productionPlanId;

    @Column(name = "plan_item_id", nullable = false)
    private Long planItemId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_inventory_id", nullable = false)
    private SemiFinishedBatchInventory batchInventory;

    @Column(name = "allocated_qty", nullable = false)
    private Double allocatedQty;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "RESERVED"; // RESERVED, CONSUMED, RELEASED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
