package com.plover.backerymanagmentsystem.pos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "outlet_returns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_note_id", unique = true, nullable = false)
    private String returnNoteId;

    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutletReturnStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "initiator_id")
    private UUID initiatorId;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "receiver_id")
    private UUID receiverId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "outletReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutletReturnItem> items;
}
