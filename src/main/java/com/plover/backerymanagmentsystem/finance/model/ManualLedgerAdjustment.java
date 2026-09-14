package com.plover.backerymanagmentsystem.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a manual ledger adjustment for a supplier. Positive
 * amounts represent debits (e.g. correcting under-billing) while negative
 * amounts represent credits (e.g. discounts, write-offs). Manual adjustments
 * are restricted to the Finance role and require remarks.
 */
@Entity
@Table(name = "manual_ledger_adjustments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualLedgerAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adjustment_id")
    private Long adjustmentId;

    @Column(name = "adjustment_ref", unique = true, nullable = false, length = 50)
    private String adjustmentRef;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDate adjustmentDate;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "remarks", nullable = false, length = 500)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;
}
