package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.GtnItem;
import com.plover.backerymanagmentsystem.pos.model.GtnStatus;

/**
 * Repository interface for GTN Item entities
 */
@Repository
public interface GtnItemRepository extends JpaRepository<GtnItem, Integer> {

    /**
     * Find all GTN items by GTN ID with product information
     *
     * @param gtnId The GTN ID to search for
     * @return List of GTN items with product details
     */
    @Query("SELECT gi FROM GtnItem gi "
            + "JOIN FETCH gi.product p "
            + "WHERE gi.gtn.gtnId = :gtnId "
            + "ORDER BY p.productName")
    List<GtnItem> findByGtnIdWithProduct(@Param("gtnId") Integer gtnId);

    /**
     * Find all GTN items by multiple GTN IDs with product information
     *
     * @param gtnIds List of GTN IDs to search for
     * @return List of GTN items with product details
     */
    @Query("SELECT gi FROM GtnItem gi "
            + "JOIN FETCH gi.product p "
            + "WHERE gi.gtn.gtnId IN :gtnIds "
            + "ORDER BY gi.gtn.gtnId, p.productName")
    List<GtnItem> findByGtnIdsWithProduct(@Param("gtnIds") List<Integer> gtnIds);

    /**
     * Find all GTN items with partially received status with full details
     *
     * @return List of partially received GTN items with GTN and product details
     */
    @Query("SELECT gi FROM GtnItem gi "
            + "JOIN FETCH gi.gtn g "
            + "JOIN FETCH gi.product p "
            + "WHERE gi.status = :status "
            + "ORDER BY g.date DESC, p.productName")
    List<GtnItem> findByStatusWithDetails(@Param("status") GtnStatus status);

    /**
     * Find GTN items by GTN ID and item IDs
     *
     * @param gtnId The GTN ID
     * @param itemIds List of GTN item IDs
     * @return List of GTN items
     */
    @Query("SELECT gi FROM GtnItem gi "
            + "WHERE gi.gtn.gtnId = :gtnId AND gi.gtnItemId IN :itemIds")
    List<GtnItem> findByGtnIdAndItemIds(@Param("gtnId") Integer gtnId, @Param("itemIds") List<Integer> itemIds);
}
