package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.ProductionStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Integer> {
    java.util.Optional<ProductionStage> findByProductionStageIgnoreCase(String productionStage);
}
