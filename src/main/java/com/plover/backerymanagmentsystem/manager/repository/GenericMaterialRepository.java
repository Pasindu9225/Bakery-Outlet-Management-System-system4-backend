package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.GenericMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenericMaterialRepository extends JpaRepository<GenericMaterial, Long> {
    Optional<GenericMaterial> findByName(String name);
    java.util.List<GenericMaterial> findByCategory(String category);
}
