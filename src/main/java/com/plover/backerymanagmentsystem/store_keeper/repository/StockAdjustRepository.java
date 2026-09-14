package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjust;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;

/**
 * Repository interface for StockAdjust entity operations.
 */
@Repository
public interface StockAdjustRepository extends JpaRepository<StockAdjust, Long> {

    /**
     * Find all stock adjustments by raw material ID.
     *
     * @param rawMaterialId the raw material ID
     * @return list of stock adjustments for the raw material
     */
    List<StockAdjust> findByRawMaterialIdOrderByCreatedAtDesc(Long rawMaterialId);

    /**
     * Find all stock adjustments by status.
     *
     * @param status the adjustment status
     * @return list of stock adjustments with the given status
     */
    List<StockAdjust> findByStatusOrderByCreatedAtDesc(StockAdjustmentStatus status);

    /**
     * Find all pending stock adjustments.
     *
     * @return list of pending stock adjustments
     */
    @Query("SELECT sa FROM StockAdjust sa WHERE sa.status = 'PENDING' ORDER BY sa.createdAt DESC")
    List<StockAdjust> findAllPendingAdjustments();

    /**
     * Count pending adjustments for a specific raw material.
     *
     * @param rawMaterialId the raw material ID
     * @return count of pending adjustments
     */
    @Query("SELECT COUNT(sa) FROM StockAdjust sa WHERE sa.rawMaterial.id = :rawMaterialId AND sa.status = 'PENDING'")
    Long countPendingAdjustmentsByRawMaterial(@Param("rawMaterialId") Long rawMaterialId);

    /**
     * Find all stock adjustments with raw material data eagerly fetched.
     *
     * @return list of all stock adjustments ordered by creation date (newest
     * first)
     */
    @Query("SELECT sa FROM StockAdjust sa JOIN FETCH sa.rawMaterial JOIN FETCH sa.addedBy LEFT JOIN FETCH sa.approvedBy ORDER BY sa.createdAt DESC")
    List<StockAdjust> findAllWithRawMaterialData();

    List<StockAdjust> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
