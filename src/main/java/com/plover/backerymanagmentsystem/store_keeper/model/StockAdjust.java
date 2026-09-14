package com.plover.backerymanagmentsystem.store_keeper.model;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
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
 * Entity representing a stock adjustment record in the system. This tracks
 * adjustments made to raw material inventory for various reasons such as
 * damage, loss, counting errors, expiration, etc.
 */
@Entity
@Table(name = "stock_adjust")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjust {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", nullable = false)
    private RawMaterial rawMaterial;

    @Column(name = "change_quantity", nullable = false)
    private Double changeQuantity;

    @Column(name = "before_quantity", nullable = false)
    private Double beforeQuantity;

    @Column(name = "reason_for_adjust", nullable = false)
    @jakarta.persistence.Convert(converter = StockAdjustmentReasonConverter.class)
    private StockAdjustmentReason reasonForAdjust;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status", nullable = false)
    @jakarta.persistence.Convert(converter = StockAdjustmentStatusConverter.class)
    private StockAdjustmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private AuthModel approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by", nullable = false)
    private AuthModel addedBy;

    /**
     * Calculates the quantity after adjustment.
     *
     * @return the quantity after applying the adjustment
     */
    public Double getAfterQuantity() {
        return beforeQuantity + changeQuantity;
    }
}
