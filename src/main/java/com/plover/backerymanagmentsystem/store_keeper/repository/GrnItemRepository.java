package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.store_keeper.model.GrnItem;

/**
 * Repository interface for GrnItem entity operations.
 */
@Repository
public interface GrnItemRepository extends JpaRepository<GrnItem, Long> {

    /**
     * Find all GRN items by GRN ID.
     *
     * @param grnId the GRN ID
     * @return List of GRN items for the specified GRN
     */
    List<GrnItem> findByGrnId(Long grnId);

    /**
     * Find all GRN items by raw material ID.
     *
     * @param rawMaterialId the raw material ID
     * @return List of GRN items for the specified raw material
     */
    List<GrnItem> findByRawMaterialId(Long rawMaterialId);

    /**
     * Find all GRN items by GRN ID and raw material ID.
     *
     * @param grnId the GRN ID
     * @param rawMaterialId the raw material ID
     * @return List of GRN items matching both criteria
     */
    List<GrnItem> findByGrnIdAndRawMaterialId(Long grnId, Long rawMaterialId);

    /**
     * Get all GRN items with raw material details for a specific GRN.
     *
     * @param grnId the GRN ID
     * @return List of GRN items with raw material details loaded
     */
    @Query("SELECT gi FROM GrnItem gi LEFT JOIN FETCH gi.rawMaterial WHERE gi.grnId = :grnId")
    List<GrnItem> findByGrnIdWithRawMaterial(@Param("grnId") Long grnId);

    /**
     * Delete all GRN items by GRN ID.
     *
     * @param grnId the GRN ID
     */
    void deleteByGrnId(Long grnId);
}
