package com.plover.backerymanagmentsystem.manager.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "production_batches")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductionBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_plan_item_id", nullable = false)
    private Long productionPlanItemId;

    @Column(name = "produced_qty", nullable = false)
    private Integer producedQty;

    @Column(name = "exact_produced_qty")
    private Double exactProducedQty;

    @Column(name = "wastage_qty", nullable = false)
    @Builder.Default
    private Integer wastageQty = 0;

    @Column(name = "wastage_reason", length = 500)
    private String wastageReason;

    @Column(name = "produced_by", length = 16, columnDefinition = "BINARY(16)")
    private byte[] producedBy;

    @Column(name = "production_center_id", nullable = false)
    private Long productionCenterId;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
