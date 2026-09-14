package com.plover.backerymanagmentsystem.pos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.DayProduction;

/**
 * Repository interface for DayProduction entities
 */
@Repository
public interface DayProductionRepository extends JpaRepository<DayProduction, Integer> {

    @Query("SELECT dp FROM DayProduction dp WHERE dp.orderedDate = :orderedDate AND dp.isActive = true ORDER BY dp.productionId DESC")
    List<DayProduction> findByOrderedDateAndIsActiveTrue(@Param("orderedDate") LocalDate orderedDate);

    @Query("SELECT dp FROM DayProduction dp WHERE dp.orderedDate = :orderedDate AND dp.isActive = true AND (:outletId IS NULL OR dp.outletId = :outletId) ORDER BY dp.productionId DESC")
    List<DayProduction> findByOrderedDateAndOutletIdAndIsActiveTrue(@Param("orderedDate") LocalDate orderedDate, @Param("outletId") Long outletId);
}
