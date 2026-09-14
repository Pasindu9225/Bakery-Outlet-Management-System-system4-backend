package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;

/**
 * Repository interface for Grn entity operations.
 */
@Repository
public interface GrnRepository extends JpaRepository<Grn, Long> {

    /**
     * Find GRN by ID with pessimistic write lock.
     *
     * @param grnId the GRN ID
     * @return Optional containing the locked GRN if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Grn g WHERE g.grnId = :grnId")
    Optional<Grn> findByGrnIdForUpdate(@Param("grnId") Long grnId);

    /**
     * Find GRN by purchase order ID.
     *
     * @param poId the purchase order ID
     * @return Optional containing the GRN if found
     */
    Optional<Grn> findByPoId(Long poId);

    /**
     * Find all GRNs by supplier ID.
     *
     * @param supplierId the supplier ID
     * @return List of GRNs for the supplier
     */
    List<Grn> findBySupplierId(Long supplierId);

    /**
     * Find all GRNs by status.
     *
     * @param status the GRN status
     * @return List of GRNs with the specified status
     */
    List<Grn> findByGrnStatus(GrnStatus status);

    /**
     * Find all GRNs by supplier and status.
     *
     * @param supplierId the supplier ID
     * @param status the GRN status
     * @return List of GRNs for the supplier with the specified status
     */
    List<Grn> findBySupplierIdAndGrnStatus(Long supplierId, GrnStatus status);

    /**
     * Check if a GRN exists for a purchase order.
     *
     * @param poId the purchase order ID
     * @return true if GRN exists, false otherwise
     */
    boolean existsByPoId(Long poId);

    /**
     * Get all pending GRNs with their items for a specific supplier.
     *
     * @param supplierId the supplier ID
     * @return List of pending GRNs with items loaded
     */
    @Query("SELECT g FROM Grn g LEFT JOIN FETCH g.grnItems WHERE g.supplierId = :supplierId AND g.grnStatus = 'PENDING'")
    List<Grn> findPendingGrnsWithItemsBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * Get all GRNs with their item counts and supplier information. Returns GRN
     * data along with the count of items in each GRN. Only returns GRNs where
     * the associated Purchase Order has status 'Approved'.
     *
     * @return List of Object arrays containing GRN data and item counts
     */
    @Query("SELECT g.grnId, g.receivedDate, g.poId, g.supplierId, s.name, g.total, g.grnStatus, g.isReceived, g.invoiceNumber, "
            + "(SELECT COALESCE(COUNT(gi.grnItemId), 0) FROM GrnItem gi WHERE gi.grnId = g.grnId) as itemCount, "
            + "po.poId as poReference, po.estimatedDeliveryDate, g.createdAt, g.storekeeperSignature "
            + "FROM Grn g "
            + "LEFT JOIN g.supplier s "
            + "LEFT JOIN g.purchaseOrder po "
            + "WHERE UPPER(po.status) = 'APPROVED' "
            + "ORDER BY g.grnId DESC")
    List<Object[]> findAllGrnsWithItemCounts();

    /**
     * Retrieve all GRNs with their items and related supplier details for
     * partial receipt tracking.
     *
     * @return list of GRNs with items and supplier eagerly fetched
     */
    @Query("SELECT DISTINCT g FROM Grn g "
            + "LEFT JOIN FETCH g.grnItems gi "
            + "LEFT JOIN FETCH gi.rawMaterial "
            + "LEFT JOIN FETCH g.supplier s "
            + "WHERE g.grnItems IS NOT EMPTY")
    List<Grn> findAllWithItemsAndSupplier();
}
