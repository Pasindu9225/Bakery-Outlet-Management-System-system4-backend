package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.ActualProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActualProductionRepository extends JpaRepository<ActualProduction, Long> {
    Optional<ActualProduction> findByProductId(Long productId);
}
