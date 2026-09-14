package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;

@Repository
public interface ProductionCenterRepository extends JpaRepository<ProductionCenter, Long> {
    
    @Query("SELECT p FROM ProductionCenter p WHERE p.isActive = true AND (p.establishedDate IS NULL OR p.establishedDate <= CURRENT_DATE)")
    List<ProductionCenter> findActiveAndEstablished();

    @Query("SELECT p FROM ProductionCenter p WHERE p.type = :type AND p.isActive = true AND (p.establishedDate IS NULL OR p.establishedDate <= CURRENT_DATE)")
    List<ProductionCenter> findActiveAndEstablishedByType(@org.springframework.data.repository.query.Param("type") ProductionCenterType type);

    List<ProductionCenter> findByTypeAndIsActiveTrue(ProductionCenterType type);
}
