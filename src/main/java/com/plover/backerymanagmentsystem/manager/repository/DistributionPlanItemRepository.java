package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.DistributionPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributionPlanItemRepository extends JpaRepository<DistributionPlanItem, Long> {
    
    List<DistributionPlanItem> findByDistributionPlan_DpId(Long dpId);
}
