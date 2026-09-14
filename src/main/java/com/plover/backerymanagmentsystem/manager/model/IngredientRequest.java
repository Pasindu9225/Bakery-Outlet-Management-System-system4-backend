package com.plover.backerymanagmentsystem.manager.model;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "ingredient_requests")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IngredientRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_plan_id")
    private Long productionPlanId;

    @Column(name = "production_center_id", nullable = false)
    private Long productionCenterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IngredientRequestStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<IngredientRequestItem> items;
}
