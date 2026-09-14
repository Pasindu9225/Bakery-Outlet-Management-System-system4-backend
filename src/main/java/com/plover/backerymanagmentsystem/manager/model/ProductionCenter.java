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
@Table(name = "production_center")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "center_name", nullable = false)
    private String centerName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "location")
    private String location;

    @Column(name = "established_date")
    private java.time.LocalDate establishedDate;

    @Column(name = "is_active", columnDefinition = "TINYINT(1)")
    private Boolean isActive;

    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "mini_store_id")
    private MiniStore miniStore;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'BAKERY'")
    private ProductionCenterType type;
}
