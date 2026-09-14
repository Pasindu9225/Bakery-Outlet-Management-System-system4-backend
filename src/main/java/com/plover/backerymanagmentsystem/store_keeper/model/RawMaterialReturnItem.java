package com.plover.backerymanagmentsystem.store_keeper.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing individual items in a raw material return.
 */
@Entity
@Table(name = "raw_material_return_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "return_id", nullable = false)
    private Long returnId;

    @Column(name = "raw_material_id", nullable = false)
    private Long rawMaterialId;

    @Column(name = "return_quantity", nullable = false)
    private Double returnQuantity;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnStatus status;

    @jakarta.persistence.Convert(converter = ReturnReasonConverter.class)
    @Column(name = "reason", nullable = false)
    private ReturnReason reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", referencedColumnName = "return_id", insertable = false, updatable = false)
    private RawMaterialReturn rawMaterialReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", referencedColumnName = "id", insertable = false, updatable = false)
    private RawMaterial rawMaterial;

    /**
     * Enumeration for return item status.
     */
    public enum ReturnStatus {
        APPROVED,
        NOT_APPROVED,
        RETURNED,
        REJECTED
    }
}
