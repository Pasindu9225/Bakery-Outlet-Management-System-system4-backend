package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialSupplier;

/**
 * Repository interface for RawMaterialSupplier entity operations.
 */
@Repository
public interface RawMaterialSupplierRepository extends JpaRepository<RawMaterialSupplier, Long> {

    /**
     * Finds all suppliers for a specific raw material, ordered by preference
     * and cost.
     *
     * @param rawMaterialId the raw material ID to search for
     * @return List of raw material suppliers for the given material
     */
    @Query("SELECT rms FROM RawMaterialSupplier rms "
            + "JOIN FETCH rms.supplier s "
            + "JOIN FETCH rms.rawMaterial rm "
            + "WHERE rms.rawMaterialId = :rawMaterialId "
            + "ORDER BY rms.isPreferred DESC, rms.negotiatedUnitCost ASC, s.name ASC")
    List<RawMaterialSupplier> findByRawMaterialIdWithDetails(@Param("rawMaterialId") Long rawMaterialId);

    /**
     * Checks if any suppliers exist for a specific raw material.
     *
     * @param rawMaterialId the raw material ID to check
     * @return true if suppliers exist for the material
     */
    boolean existsByRawMaterialId(Long rawMaterialId);

    /**
     * Finds all raw material suppliers for a specific raw material.
     *
     * @param rawMaterialId the raw material ID to search for
     * @return List of raw material suppliers
     */
    List<RawMaterialSupplier> findByRawMaterialId(Long rawMaterialId);

    /**
     * Finds a specific supplier-material relationship.
     *
     * @param rawMaterialId the raw material ID
     * @param supplierId the supplier ID
     * @return Optional of the raw material supplier relationship
     */
    @Query("SELECT rms FROM RawMaterialSupplier rms "
            + "JOIN FETCH rms.supplier s "
            + "JOIN FETCH rms.rawMaterial rm "
            + "WHERE rms.rawMaterialId = :rawMaterialId "
            + "AND rms.supplierId = :supplierId")
    Optional<RawMaterialSupplier> findByRawMaterialIdAndSupplierId(
            @Param("rawMaterialId") Long rawMaterialId,
            @Param("supplierId") Long supplierId);

    /**
     * Checks if a supplier can supply a specific raw material.
     *
     * @param rawMaterialId the raw material ID
     * @param supplierId the supplier ID
     * @return true if supplier-material relationship exists
     */
    boolean existsByRawMaterialIdAndSupplierId(Long rawMaterialId, Long supplierId);

    /**
     * Finds all raw materials supplied by a specific supplier with details.
     *
     * @param supplierId the supplier ID to search for
     * @return List of raw material suppliers for the given supplier
     */
    @Query("SELECT rms FROM RawMaterialSupplier rms "
            + "JOIN FETCH rms.supplier s "
            + "JOIN FETCH rms.rawMaterial rm "
            + "WHERE rms.supplierId = :supplierId "
            + "ORDER BY rms.isPreferred DESC, rm.brand.genericMaterial.name ASC")
    List<RawMaterialSupplier> findBySupplierId(@Param("supplierId") Long supplierId);
}
