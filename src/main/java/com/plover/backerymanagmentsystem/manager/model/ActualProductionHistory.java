package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "actual_production_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualProductionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActualProductionHistoryAction actionType;

    @Column(name = "reference_name", length = 255)
    private String referenceName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
