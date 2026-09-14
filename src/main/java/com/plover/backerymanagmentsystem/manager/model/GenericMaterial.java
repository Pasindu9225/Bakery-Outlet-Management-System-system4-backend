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
@Table(name = "generic_materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenericMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;

    @Column(name = "category")
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "minimum_stock_level")
    private Double minimumStockLevel;

    @Column(name = "max_stock_level")
    private Double maxStockLevel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
