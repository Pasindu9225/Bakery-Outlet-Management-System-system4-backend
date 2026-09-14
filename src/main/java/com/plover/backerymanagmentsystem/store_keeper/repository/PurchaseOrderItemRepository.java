package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;

/**
 * Repository interface for PurchaseOrderItem entity operations.
 */
@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    /**
     * Finds purchase order items by raw material ID.
     *
     * @param rawMaterialId the raw material ID to search for
     * @return List of purchase order items for the given raw material
     */
    List<PurchaseOrderItem> findByRawMaterialId(Integer rawMaterialId);

    /**
     * Finds the most recent purchase order item by raw material ID.
     *
     * @param rawMaterialId the raw material ID to search for
     * @return Optional of the most recent purchase order item
     */
    @Query("SELECT poi FROM PurchaseOrderItem poi "
            + "JOIN FETCH poi.purchaseOrder po "
            + "WHERE poi.rawMaterialId = :rawMaterialId "
            + "ORDER BY po.estimatedDeliveryDate DESC, poi.poiId DESC")
    Optional<PurchaseOrderItem> findMostRecentByRawMaterialId(@Param("rawMaterialId") Integer rawMaterialId);

    /**
     * Check if a purchase order exists for a specific raw material.
     *
     * @param rawMaterialId the raw material ID to check
     * @return true if a purchase order item exists for the material
     */
    boolean existsByRawMaterialId(Integer rawMaterialId);
}
