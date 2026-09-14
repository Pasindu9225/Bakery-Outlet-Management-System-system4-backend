package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "semi_finished_batch_inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemiFinishedBatchInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "mini_store_id", nullable = false)
    private Long miniStoreId;

    @Column(name = "initial_qty", nullable = false)
    private Double initialQty;

    @Column(name = "available_qty", nullable = false)
    private Double availableQty;

    @Column(name = "reserved_qty", nullable = false)
    @Builder.Default
    private Double reservedQty = 0.0;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(name = "manufactured_date")
    private LocalDateTime manufacturedDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }

    public Double getUnreservedQty() {
        if (isExpired()) return 0.0;
        double unreserved = (availableQty != null ? availableQty : 0.0) - (reservedQty != null ? reservedQty : 0.0);
        return Math.max(0.0, unreserved);
    }
}
