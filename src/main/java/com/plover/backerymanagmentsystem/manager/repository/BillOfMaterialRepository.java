package com.plover.backerymanagmentsystem.manager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;

@Repository
public interface BillOfMaterialRepository extends JpaRepository<BillOfMaterial, Long> {

    List<BillOfMaterial> findByParentProductIdAndIsActiveTrue(Long parentProductId);

    @Modifying
    @Query("DELETE FROM BillOfMaterial b WHERE b.parentProductId = :parentProductId")
    void deleteByParentProductId(@Param("parentProductId") Long parentProductId);

    boolean existsByParentProductId(Long parentProductId);

    boolean existsByChildItemIdAndChildType(Long childItemId, BillOfMaterial.ChildType childType);

    List<BillOfMaterial> findByChildItemIdAndChildType(Long childItemId, BillOfMaterial.ChildType childType);
}
