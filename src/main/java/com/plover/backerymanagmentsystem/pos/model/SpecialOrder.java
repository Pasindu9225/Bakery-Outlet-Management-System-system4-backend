package com.plover.backerymanagmentsystem.pos.model;

import com.plover.backerymanagmentsystem.manager.model.Customer;
import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a Special Order (Customer Pre-orders with Advances)
 */
@Entity
@Table(name = "special_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "advance_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal advanceAmount;

    @Column(name = "balance_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAmount;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SpecialOrderStatus status;

    @Column(name = "cashier_id", nullable = false)
    private UUID cashierId;

    @Column(name = "approver_id")
    private UUID approverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AuthModel cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AuthModel approver;

    @OneToMany(mappedBy = "specialOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SpecialOrderItem> items;

    @Column(name = "outlet_id")
    private Long outletId;

    @Column(name = "manager_verification_code")
    private String managerVerificationCode;

    @Column(name = "verified_by_manager_name")
    private String verifiedByManagerName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
