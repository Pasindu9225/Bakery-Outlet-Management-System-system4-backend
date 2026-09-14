package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.ProductionPlanItem;

@Repository
public interface ProductionPlanItemRepository extends JpaRepository<ProductionPlanItem, Long> {

    // Method to fetch productIds by productionPlanId
    @Query("SELECT p.productId FROM ProductionPlanItem p WHERE p.productionPlan.id = :productionPlanId")
    List<Long> findProductIdsByProductionPlanId(Long productionPlanId);

    @Query("SELECT DISTINCT pc FROM ProductionPlanItem p JOIN ProductionCenter pc ON p.productionCenterId = pc.id WHERE p.productId = :productId")
    List<com.plover.backerymanagmentsystem.manager.model.ProductionCenter> findProductionCentersByProductId(Long productId);

    List<ProductionPlanItem> findByProductionPlan_Id(Long productionPlanId);

    List<ProductionPlanItem> findByParentPlanItemId(Long parentPlanItemId);

    List<ProductionPlanItem> findByProductionPlan_IdAndParentPlanItemId(Long productionPlanId, Long parentPlanItemId);
}
