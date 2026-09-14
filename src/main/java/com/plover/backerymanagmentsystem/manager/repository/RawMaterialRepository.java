package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;

@Repository
public interface RawMaterialRepository extends JpaRepository<RawMaterial, Long> {

    List<RawMaterial> findAllByOrderByIdDesc();

    @Query("SELECT r FROM RawMaterial r WHERE r.brand.genericMaterial.name = :materialName")
    Optional<RawMaterial> findByMaterialName(@Param("materialName") String materialName);

    @Query("SELECT r FROM RawMaterial r WHERE r.brand.genericMaterial.name = :genericName")
    List<RawMaterial> findAllByGenericMaterialName(@Param("genericName") String genericName);

    Optional<RawMaterial> findByMaterialCode(String materialCode);
    List<RawMaterial> findAllByMaterialCode(String materialCode);
    List<RawMaterial> findByMaterialCodeStartingWith(String prefix);

    Optional<RawMaterial> findByMaterialCodeAndBatchNo(String materialCode, String batchNo);

    List<RawMaterial> findByIsActiveTrue();

    @Query("SELECT r FROM RawMaterial r WHERE r.isActive = true ORDER BY r.brand.genericMaterial.name ASC")
    List<RawMaterial> findByIsActiveTrueOrderByMaterialNameAsc();

    @Query("SELECT r FROM RawMaterial r WHERE r.brand.genericMaterial.category = :category")
    List<RawMaterial> findByCategory(@Param("category") String category);

    @Query("SELECT r FROM RawMaterial r WHERE r.currentStock <= r.minimumStockLevel AND r.isActive = true")
    List<RawMaterial> findLowStockMaterials();

    @Query("SELECT r FROM RawMaterial r WHERE r.brand.genericMaterial.name LIKE %:searchTerm% OR r.description LIKE %:searchTerm%")
    List<RawMaterial> findBySearchTerm(@Param("searchTerm") String searchTerm);

    @Query("SELECT DISTINCT r.brand.genericMaterial.category FROM RawMaterial r WHERE r.brand.genericMaterial.category IS NOT NULL")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT r.brand.name FROM RawMaterial r WHERE r.brand IS NOT NULL")
    List<String> findDistinctBrands();

    @Query("SELECT gm.id as id, gm.name as name, " +
           "gm.category as category, gm.unitOfMeasure as uom, " +
           "b.name as brandName, SUM(r.currentStock) as brandStock " +
           "FROM RawMaterial r " +
           "LEFT JOIN r.brand b " +
           "LEFT JOIN b.genericMaterial gm " +
           "GROUP BY gm.id, gm.name, gm.category, gm.unitOfMeasure, b.name")
    List<Object[]> findAggregatedStock();
}
