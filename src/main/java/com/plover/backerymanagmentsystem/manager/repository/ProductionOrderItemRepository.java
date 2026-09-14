package com.plover.backerymanagmentsystem.manager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.ProductionOrderItem;

@Repository
public interface ProductionOrderItemRepository extends JpaRepository<ProductionOrderItem, Long> {

    long countByProductionCenterId(Long productionCenterId);
}
