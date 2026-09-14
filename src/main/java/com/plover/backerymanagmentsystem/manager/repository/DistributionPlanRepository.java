package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.DistributionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributionPlanRepository extends JpaRepository<DistributionPlan, Long> {
    
    List<DistributionPlan> findAllByOrderByDateDesc();
    
    List<DistributionPlan> findByOutlet_OutletId(Long outletId);
    
    List<DistributionPlan> findByName(String name);

    List<DistributionPlan> findByProductionPlan_Id(Long productionPlanId);
    
    // Native query that avoids comparing date with invalid literals and returns date as string plus status
    @Query(value = "SELECT dp_id, CAST(date AS CHAR) AS date_str, is_active, name, outlet_id, status FROM distributhio_plans", 
           nativeQuery = true)
    List<Object[]> findAllWithValidDatesNative();
}
