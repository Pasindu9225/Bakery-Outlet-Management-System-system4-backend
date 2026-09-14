package com.plover.backerymanagmentsystem.pos.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outlet_transfer_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletTransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_outlet_id", nullable = false)
    private Long sourceOutletId;

    @Column(name = "destination_outlet_id", nullable = false)
    private Long destinationOutletId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private com.plover.backerymanagmentsystem.manager.model.Product product;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "approved_quantity")
    private Integer approvedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutletTransferRequestStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
