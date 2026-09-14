package com.plover.backerymanagmentsystem.manager.model;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "kitchen_returns")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KitchenReturn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_number", unique = true, nullable = false, length = 50)
    private String returnNumber;

    @Column(name = "source_production_center_id", nullable = false)
    private Long sourceProductionCenterId;

    @Column(name = "returned_by", length = 16, columnDefinition = "BINARY(16)")
    private byte[] returnedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private KitchenReturnStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "kitchenReturn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<KitchenReturnItem> items;
}
