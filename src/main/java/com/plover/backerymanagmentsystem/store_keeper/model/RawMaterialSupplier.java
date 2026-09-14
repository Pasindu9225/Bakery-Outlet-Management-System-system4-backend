package com.plover.backerymanagmentsystem.store_keeper.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;

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
 * Entity representing the relationship between Raw Materials and Suppliers.
 */
@Entity
@Table(name = "raw_material_suppliers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "raw_material_id", nullable = false)
    private Long rawMaterialId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "negotiated_unit_cost")
    private Double negotiatedUnitCost;

    @Column(name = "is_preferred", nullable = false)
    private Boolean isPreferred = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", referencedColumnName = "id", insertable = false, updatable = false)
    private RawMaterial rawMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id", insertable = false, updatable = false)
    private Supplier supplier;
}
