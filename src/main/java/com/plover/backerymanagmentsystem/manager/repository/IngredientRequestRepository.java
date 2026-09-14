package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.IngredientRequest;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus;

@Repository
public interface IngredientRequestRepository extends JpaRepository<IngredientRequest, Long> {
    List<IngredientRequest> findByProductionCenterIdOrderByCreatedAtDesc(Long pcId);
    List<IngredientRequest> findByProductionCenterIdOrProductionCenterIdIsNullOrderByCreatedAtDesc(Long pcId);
    List<IngredientRequest> findByStatusOrderByCreatedAtAsc(IngredientRequestStatus status);
    long countByStatus(IngredientRequestStatus status);
    List<IngredientRequest> findByProductionPlanId(Long planId);
    boolean existsByProductionPlanIdAndProductionCenterId(Long planId, Long productionCenterId);
    List<IngredientRequest> findByProductionCenterIdAndStatus(Long productionCenterId, IngredientRequestStatus status);
}
