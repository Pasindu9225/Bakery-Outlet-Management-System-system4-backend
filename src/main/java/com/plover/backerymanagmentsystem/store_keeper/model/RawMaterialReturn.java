package com.plover.backerymanagmentsystem.store_keeper.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity representing a raw material return transaction.
 */
@Entity
@Table(name = "raw_material_return")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "number_of_items", nullable = false)
    private Integer numberOfItems;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id", insertable = false, updatable = false)
    private Supplier supplier;

    @OneToMany(mappedBy = "rawMaterialReturn", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RawMaterialReturnItem> returnItems;
}
