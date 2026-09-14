package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.RawMaterialRequirement;

@Repository
public interface RawMaterialRequirementRepository extends JpaRepository<RawMaterialRequirement, Long> {

    List<RawMaterialRequirement> findByProductionOrder_Id(Long productionOrderId);

    List<RawMaterialRequirement> findByProductionOrder_ProductionPlan_Id(Long productionPlanId);

    @Query("SELECT r FROM RawMaterialRequirement r WHERE r.productionOrder.productionPlan.id = :planId AND r.rawMaterial.id = :materialId")
    List<RawMaterialRequirement> findByProductionPlanIdAndRawMaterialId(@Param("planId") Long planId, @Param("materialId") Long materialId);

    boolean existsByProductionOrder_ProductionPlan_Id(Long productionPlanId);
}
