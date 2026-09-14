package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    List<ProductionPlan> findByStatus(ProductionPlan.ProductionPlanStatus status);
    List<ProductionPlan> findByStatusIn(List<ProductionPlan.ProductionPlanStatus> statuses);

    List<ProductionPlan> findByPlanDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT p FROM ProductionPlan p WHERE p.planName LIKE %:searchTerm% OR p.notes LIKE %:searchTerm%")
    List<ProductionPlan> findBySearchTerm(@Param("searchTerm") String searchTerm);

    List<ProductionPlan> findByOrderByCreatedAtDesc();

    List<ProductionPlan> findByIsTemplateTrue();

    @Query(
        "SELECT DISTINCT pp FROM ProductionPlan pp " +
        "WHERE pp.isTemplate = false " +
        "AND pp.status != com.plover.backerymanagmentsystem.manager.model.ProductionPlan.ProductionPlanStatus.CANCELLED " +
        "ORDER BY pp.planDate DESC"
    )
    List<ProductionPlan> findActivePlansByProductionCenterId(@Param("pcId") Long pcId);

    List<ProductionPlan> findByDepartment(String department);
}
