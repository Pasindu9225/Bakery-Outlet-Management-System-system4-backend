package com.plover.backerymanagmentsystem.pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.GtnDayProduction;

/**
 * Repository interface for GtnDayProduction entities
 */
@Repository
public interface GtnDayProductionRepository extends JpaRepository<GtnDayProduction, Integer> {
}
