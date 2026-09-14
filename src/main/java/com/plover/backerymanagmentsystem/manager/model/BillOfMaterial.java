package com.plover.backerymanagmentsystem.manager.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bill_of_materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillOfMaterial {

    public enum ChildType { raw_material, product }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_product_id", nullable = false)
    private Long parentProductId;

    @Column(name = "child_item_id", nullable = false)
    private Long childItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "child_type", nullable = false)
    private ChildType childType;

    @Column(name = "quantity", nullable = false)
    private java.math.BigDecimal quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "production_center_id")
    private Long productionCenterId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


