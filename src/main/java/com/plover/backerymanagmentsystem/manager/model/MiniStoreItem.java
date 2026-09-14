package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mini_store_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniStoreItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id", columnDefinition = "INT")
    private Integer itemId;
    
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    
    @Column(name = "raw_material_id", columnDefinition = "BIGINT")
    private Long rawMaterialId;
    
    @Column(name = "system_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal systemQty;
    
    @Column(name = "physical_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal physicalQty;
    
    @Column(name = "variance", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal variance;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mini_store_id", nullable = false)
    private MiniStore miniStore;
    
    @Column(name = "outlet_id", columnDefinition = "BIGINT")
    private Long outletId;
    
    @Column(name = "product_id", columnDefinition = "BIGINT")
    private Long productId;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
