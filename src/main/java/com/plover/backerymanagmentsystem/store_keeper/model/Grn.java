package com.plover.backerymanagmentsystem.store_keeper.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;

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
 * Entity representing a Goods Receipt Note (GRN) in the system. A GRN is
 * created when a purchase order is placed and tracks the receipt of goods.
 */
@Entity
@Table(name = "grn")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grn_id")
    private Long grnId;

    @Column(name = "received_date")
    private LocalDateTime receivedDate;

    @Column(name = "po_id", nullable = false)
    private Long poId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "total", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "grn_status", nullable = false)
    @Builder.Default
    private GrnStatus grnStatus = GrnStatus.PENDING;

    @Column(name = "is_recieved", nullable = false)
    @Builder.Default
    private Boolean isReceived = false;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "storekeeper_signature", columnDefinition = "LONGTEXT")
    private String storekeeperSignature;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", referencedColumnName = "po_id", insertable = false, updatable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id", insertable = false, updatable = false)
    private Supplier supplier;

    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GrnItem> grnItems;
}
