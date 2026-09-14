package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.ProductionPlanBatchAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionPlanBatchAllocationRepository extends JpaRepository<ProductionPlanBatchAllocation, Long> {

    List<ProductionPlanBatchAllocation> findByProductionPlanId(Long productionPlanId);

    List<ProductionPlanBatchAllocation> findByPlanItemId(Long planItemId);

    List<ProductionPlanBatchAllocation> findByProductionPlanIdAndStatus(Long productionPlanId, String status);
}
