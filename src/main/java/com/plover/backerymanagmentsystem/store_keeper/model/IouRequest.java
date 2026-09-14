package com.plover.backerymanagmentsystem.store_keeper.model;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "iou_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IouRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "justification", length = 1000)
    private String justification;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IouStatus status;

    @Column(name = "total_estimated_amount")
    private Double totalEstimatedAmount;

    @Column(name = "issued_amount")
    private Double issuedAmount;

    @Column(name = "total_actual_amount")
    private Double totalActualAmount;

    @Column(name = "difference_amount")
    private Double differenceAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "invoice_number")
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by", nullable = false)
    private AuthModel addedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AuthModel approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_approved_by")
    private AuthModel finalApprovedBy;

    @OneToMany(mappedBy = "iouRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IouRequestItem> items = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
