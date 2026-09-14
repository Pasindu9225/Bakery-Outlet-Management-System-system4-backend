package com.plover.backerymanagmentsystem.manager.model;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transfer_notes")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransferNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_number", unique = true, nullable = false, length = 50)
    private String transferNumber;

    @Column(name = "source_production_center_id", nullable = false)
    private Long sourceProductionCenterId;

    @Column(name = "destination_outlet_id")
    private Long destinationOutletId;

    @Column(name = "destination_mpc_id")
    private Long destinationMpcId;

    @Column(name = "transferred_by", length = 16, columnDefinition = "BINARY(16)")
    private byte[] transferredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransferNoteStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "transferNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TransferNoteItem> items;
}
