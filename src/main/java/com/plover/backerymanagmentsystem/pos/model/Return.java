package com.plover.backerymanagmentsystem.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing an item return or exchange transaction
 */
@Entity
@Table(name = "returns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Return {

    public enum RefundType {
        REFUND, EXCHANGE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Integer returnId;

    @Column(name = "sale_id", nullable = true)
    private Integer saleId;

    @Column(name = "return_reason", nullable = false)
    private String returnReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false)
    private RefundType refundType;

    @Column(name = "cashier_id", nullable = false)
    private UUID cashierId;

    @Column(name = "payment_method_id", nullable = true)
    private Integer paymentMethodId;

    @Column(name = "total_return_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalReturnAmount;

    @Column(name = "total_exchange_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalExchangeAmount;

    @Column(name = "net_refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netRefundAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", referencedColumnName = "sale_id", insertable = false, updatable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AuthModel cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", referencedColumnName = "payment_method_id", insertable = false, updatable = false)
    private PaymentMethod paymentMethod;

    @OneToMany(mappedBy = "returnRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnItem> returnItems;
}
