package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.ProductionPlanMaterials;

@Repository
public interface ProductionPlanMaterialsRepository extends JpaRepository<ProductionPlanMaterials, Long> {

    List<ProductionPlanMaterials> findByProductionPlan_Id(Long productionPlanId);

    List<ProductionPlanMaterials> findByProductionPlan_IdAndRawMaterialQuantityForKitchenNotNull(Long productionPlanId);

    List<ProductionPlanMaterials> findByProductionPlan_IdAndRawMaterialQuantityForBakeryNotNull(Long productionPlanId);

    void deleteByProductionPlan_Id(Long productionPlanId);
}


