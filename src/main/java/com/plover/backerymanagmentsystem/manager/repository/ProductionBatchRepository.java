package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.plover.backerymanagmentsystem.manager.model.ProductionBatch;

@Repository
public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {
    List<ProductionBatch> findByProductionPlanItemIdOrderByCreatedAtDesc(Long itemId);
    List<ProductionBatch> findByProductionCenterIdOrderByCreatedAtDesc(Long pcId);
}
