package com.plover.backerymanagmentsystem.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a payment made to a supplier. A payment may be allocated
 * across multiple GRNs or unfulfilled POs through {@link SupplierPaymentAllocation}.
 */
@Entity
@Table(name = "supplier_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_ref", unique = true, nullable = false, length = 50)
    private String paymentRef;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "remarks", length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<SupplierPaymentAllocation> allocations;
}
