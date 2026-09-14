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
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, unique = true)
    private String productName;

    @Column(name = "product_code", unique = true)
    private String productCode;

    @Column(name = "description")
    private String description;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category", referencedColumnName = "id")
    private Category categoryRef;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "is_kot_enabled", nullable = false)
    @Builder.Default
    private Boolean isKotEnabled = false;

    @Column(name = "is_fast_moving", nullable = false)
    @Builder.Default
    private Boolean isFastMoving = false;

    @Column(name = "production_center_id")
    private Long productionCenterId;

    @Column(name = "GB_margin")
    private Double gbMargin;

    @Column(name = "vat_status")
    private Boolean vatStatus;

    @Column(name = "brand")
    private String brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_stage", referencedColumnName = "id")
    private ProductionStage productionStageRef;

    @Column(name = "sale_price")
    private Double salePrice;

    @Column(name = "actual_gb")
    private Double actualGP;

    @Column(name = "max_order_qty")
    private Double maxOrderQty;

    @Column(name = "min_order_qty")
    private Double minOrderQty;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getCategory() {
        return categoryRef != null ? categoryRef.getName() : null;
    }

    public void setCategory(String categoryName) {
        if (this.categoryRef == null) {
            this.categoryRef = new Category();
        }
        this.categoryRef.setName(categoryName);
    }
}
