package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;

@Repository
public interface StoreKeeperProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    List<ProductionPlan> findByStatus(ProductionPlan.ProductionPlanStatus status);
    List<ProductionPlan> findByStatusIn(List<ProductionPlan.ProductionPlanStatus> statuses);
}
