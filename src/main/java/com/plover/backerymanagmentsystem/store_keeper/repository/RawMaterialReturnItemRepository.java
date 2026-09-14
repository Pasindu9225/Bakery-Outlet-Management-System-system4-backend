package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem.ReturnStatus;

/**
 * Repository interface for RawMaterialReturnItem entity operations.
 */
@Repository
public interface RawMaterialReturnItemRepository extends JpaRepository<RawMaterialReturnItem, Long> {

    /**
     * Finds all return items for a specific return.
     *
     * @param returnId the return ID
     * @return list of return items
     */
    List<RawMaterialReturnItem> findByReturnId(Long returnId);

    /**
     * Finds all return items for a specific raw material.
     *
     * @param rawMaterialId the raw material ID
     * @return list of return items
     */
    List<RawMaterialReturnItem> findByRawMaterialId(Long rawMaterialId);

    /**
     * Finds return items by status.
     *
     * @param status the return status
     * @return list of return items with the specified status
     */
    List<RawMaterialReturnItem> findByStatus(ReturnStatus status);

    /**
     * Finds return items for a specific return with details.
     *
     * @param returnId the return ID
     * @return list of return items with raw material details
     */
    @Query("SELECT ri FROM RawMaterialReturnItem ri "
            + "JOIN FETCH ri.rawMaterial rm "
            + "WHERE ri.returnId = :returnId "
            + "ORDER BY ri.createdAt ASC")
    List<RawMaterialReturnItem> findByReturnIdWithRawMaterialDetails(@Param("returnId") Long returnId);

    @Modifying
    @Query("UPDATE RawMaterialReturnItem ri SET ri.status = :status WHERE ri.id IN :ids AND ri.returnId = :returnId")
    int bulkUpdateStatus(@Param("returnId") Long returnId, @Param("ids") List<Long> ids, @Param("status") ReturnStatus status);

    /**
     * Finds return items by IDs with raw material details.
     *
     * @param itemIds the list of return item IDs
     * @return list of return items with raw material details
     */
    @Query("SELECT ri FROM RawMaterialReturnItem ri "
            + "JOIN FETCH ri.rawMaterial rm "
            + "WHERE ri.id IN :itemIds")
    List<RawMaterialReturnItem> findByIdInWithRawMaterialDetails(@Param("itemIds") List<Long> itemIds);

    /**
     * Updates the status of return items by IDs.
     *
     * @param itemIds the list of return item IDs
     * @param status the new status
     * @return number of updated records
     */
    @Modifying
    @Query("UPDATE RawMaterialReturnItem ri SET ri.status = :status "
            + "WHERE ri.id IN :itemIds AND ri.status = 'NOT_APPROVED'")
    int updateStatusByIdIn(@Param("itemIds") List<Long> itemIds, @Param("status") ReturnStatus status);

    /**
     * Counts return items by return ID and status.
     *
     * @param returnId the return ID
     * @param status the return status
     * @return count of items with the specified status
     */
    long countByReturnIdAndStatus(Long returnId, ReturnStatus status);
}
