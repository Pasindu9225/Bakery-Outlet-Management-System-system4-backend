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
 * Entity representing a purchase return raised against a supplier. The return
 * is linked to a specific GRN and credits the supplier ledger by the total
 * amount returned.
 */
@Entity
@Table(name = "purchase_returns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "return_ref", unique = true, nullable = false, length = 50)
    private String returnRef;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "grn_id", nullable = false)
    private Long grnId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "reason", length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "purchaseReturn", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PurchaseReturnItem> items;
}
