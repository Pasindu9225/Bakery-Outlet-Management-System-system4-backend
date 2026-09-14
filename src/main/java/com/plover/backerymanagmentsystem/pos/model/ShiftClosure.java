package com.plover.backerymanagmentsystem.pos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shift_closures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_closure_id")
    private Integer id;

    @Column(name = "cashier_id", nullable = false)
    private UUID cashierId;

    @Column(name = "outlet_id", nullable = false)
    private Integer outletId;

    @Column(name = "closure_date", nullable = false)
    private LocalDate closureDate;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = true;

    // Actual Counts (Entered by Cashier)
    @Column(name = "actual_cash", precision = 10, scale = 2, nullable = false)
    private BigDecimal actualCash;

    @Column(name = "actual_card", precision = 10, scale = 2, nullable = false)
    private BigDecimal actualCard;

    @Column(name = "actual_uber", precision = 10, scale = 2, nullable = false)
    private BigDecimal actualUber;

    @Column(name = "actual_pickme", precision = 10, scale = 2, nullable = false)
    private BigDecimal actualPickme;

    // Expected Values (Calculated by System)
    @Column(name = "expected_cash", precision = 10, scale = 2, nullable = false)
    private BigDecimal expectedCash;

    @Column(name = "expected_card", precision = 10, scale = 2, nullable = false)
    private BigDecimal expectedCard;

    @Column(name = "expected_uber", precision = 10, scale = 2, nullable = false)
    private BigDecimal expectedUber;

    @Column(name = "expected_pickme", precision = 10, scale = 2, nullable = false)
    private BigDecimal expectedPickme;

    // Reconciliation
    @Column(name = "variance", precision = 10, scale = 2, nullable = false)
    private BigDecimal variance;

    @Column(name = "cash_denominations", columnDefinition = "TEXT")
    private String cashDenominations;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AuthModel cashier;
}
