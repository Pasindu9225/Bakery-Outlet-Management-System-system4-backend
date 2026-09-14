package com.plover.backerymanagmentsystem.pos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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
 * Entity representing a sale transaction
 */
@Entity
@Table(name = "sales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Integer saleId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "sale_time", nullable = false)
    private LocalTime saleTime;

    @Column(name = "cashier_id", nullable = false)
    private UUID cashierId;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "discount_reason", length = 500)
    private String discountReason;

    @Column(name = "final_total", precision = 10, scale = 2)
    private BigDecimal finalTotal;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "invoice_printed", nullable = false)
    @Builder.Default
    private Boolean invoicePrinted = false;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "outlet_id")
    private Long outletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentCategory paymentType;

    @Column(name = "delivery_option", length = 50)
    private String deliveryOption;

    @Column(name = "customer_id")
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", insertable = false, updatable = false)
    private com.plover.backerymanagmentsystem.manager.model.Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AuthModel cashier;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleItem> saleItems;
}