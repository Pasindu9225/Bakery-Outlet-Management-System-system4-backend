package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.PackDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PackDetailsRepository extends JpaRepository<PackDetails, Long> {
    Optional<PackDetails> findByRawMaterialId(Long rawMaterialId);
}
