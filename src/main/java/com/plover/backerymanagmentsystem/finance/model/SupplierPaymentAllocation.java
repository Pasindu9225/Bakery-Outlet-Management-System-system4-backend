package com.plover.backerymanagmentsystem.finance.model;

import java.math.BigDecimal;

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

/**
 * Entity representing an allocation of a {@link SupplierPayment} to a specific
 * GRN or unfulfilled PO. Either {@code grnId} or {@code poId} must be set, but
 * never both. Validation is enforced at the service layer rather than via DB
 * constraints.
 */
@Entity
@Table(name = "supplier_payment_allocations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPaymentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "payment_id", nullable = false, insertable = false, updatable = false)
    private Long paymentId;

    @Column(name = "grn_id")
    private Long grnId;

    @Column(name = "po_id")
    private Long poId;

    @Column(name = "allocated_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", referencedColumnName = "payment_id", nullable = false)
    private SupplierPayment payment;
}
