package com.plover.backerymanagmentsystem.pos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "cash_floats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFloat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_id", nullable = false)
    private UUID cashierId;

    @Column(name = "outlet_id", nullable = false)
    private Integer outletId;

    @Column(name = "opening_balance", precision = 10, scale = 2, nullable = false)
    private BigDecimal openingBalance;

    @CreationTimestamp
    @Column(name = "opening_timestamp", updatable = false)
    private LocalDateTime openingTimestamp;

    @Column(name = "float_date", nullable = false)
    private LocalDate floatDate;

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;

    @Column(name = "closing_timestamp")
    private LocalDateTime closingTimestamp;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AuthModel cashier;
}
