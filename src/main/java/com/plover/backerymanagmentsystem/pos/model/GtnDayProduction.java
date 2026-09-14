package com.plover.backerymanagmentsystem.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity representing the relationship between GTN and DayProduction.
 */
@Entity
@Table(name = "gtn_day_production")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GtnDayProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gtn_id", nullable = false)
    private Gtn gtn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_id", nullable = false)
    private DayProduction dayProduction;
}
