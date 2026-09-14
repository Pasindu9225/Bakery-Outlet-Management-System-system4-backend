package com.plover.backerymanagmentsystem.pos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing daily production plan.
 */
@Entity
@Table(name = "day_production")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_id")
    private Integer productionId;

    @Column(name = "ordered_date", nullable = false)
    private LocalDate orderedDate;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "outlet_id")
    private Long outletId;

    @OneToMany(mappedBy = "dayProduction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DayProductionItem> dayProductionItems;
}
