package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.OutletProductionCenter;

@Repository
public interface OutletProductionCenterRepository extends JpaRepository<OutletProductionCenter, Long> {

    List<OutletProductionCenter> findByOutlet_OutletId(Long outletId);

    List<OutletProductionCenter> findByOutlet_OutletIdAndIsActiveTrue(Long outletId);

    long countByOutlet_OutletId(Long outletId);

    boolean existsByOutlet_OutletIdAndNameIgnoreCase(Long outletId, String name);
}
