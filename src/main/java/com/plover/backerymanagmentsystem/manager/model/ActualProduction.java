package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "actual_productions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualProduction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;
    
    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}
