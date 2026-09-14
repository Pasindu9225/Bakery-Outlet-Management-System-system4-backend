package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;

/**
 * Repository interface for PurchaseOrder entity operations.
 */
@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    /**
     * Finds purchase orders by supplier ID.
     *
     * @param supplierId the supplier ID to search for
     * @return List of purchase orders for the given supplier
     */
    boolean existsBySupplierId(Long supplierId);

    /**
     * Fetch purchase orders by IDs with their items and raw material details
     * eagerly loaded.
     *
     * @param poIds purchase order IDs to fetch
     * @return list of purchase orders with items and raw materials
     */
    @EntityGraph(attributePaths = {"purchaseOrderItems", "purchaseOrderItems.rawMaterial"})
    List<PurchaseOrder> findByPoIdIn(Collection<Long> poIds);

    /**
     * Fetch all purchase orders raised against a given supplier with their
     * items and raw material details eagerly loaded. Used by the FR-MIS-02
     * supplier-detail screen for the "Purchase &amp; Delivery Records" tab.
     *
     * @param supplierId supplier id
     * @return list of POs for the supplier with items and raw materials
     */
    @EntityGraph(attributePaths = {"purchaseOrderItems", "purchaseOrderItems.rawMaterial"})
    List<PurchaseOrder> findBySupplierId(Long supplierId);

    /**
     * Fetch all purchase orders ordered by ID descending to show latest first.
     */
    @EntityGraph(attributePaths = {"purchaseOrderItems", "purchaseOrderItems.rawMaterial"})
    List<PurchaseOrder> findAllByOrderByPoIdDesc();
}
