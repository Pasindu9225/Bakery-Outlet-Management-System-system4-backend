package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.ProductionOrder;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    List<ProductionOrder> findByProductionPlan_Id(Long productionPlanId);

    Optional<ProductionOrder> findByOrderNumber(String orderNumber);

    List<ProductionOrder> findByStatus(ProductionOrder.ProductionOrderStatus status);

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT po FROM ProductionOrder po " +
        "JOIN po.productionOrderItems poi " +
        "WHERE poi.productionCenterId = :pcId " +
        "ORDER BY po.orderDate DESC"
    )
    java.util.List<com.plover.backerymanagmentsystem.manager.model.ProductionOrder> findKotsByProductionCenterId(
        @org.springframework.data.repository.query.Param("pcId") Long pcId);
}
