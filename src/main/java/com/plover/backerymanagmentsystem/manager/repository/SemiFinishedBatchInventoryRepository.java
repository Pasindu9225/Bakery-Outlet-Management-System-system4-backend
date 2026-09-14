package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.SemiFinishedBatchInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SemiFinishedBatchInventoryRepository extends JpaRepository<SemiFinishedBatchInventory, Long> {

    List<SemiFinishedBatchInventory> findByProductIdAndMiniStoreId(Long productId, Long miniStoreId);

    List<SemiFinishedBatchInventory> findByMiniStoreId(Long miniStoreId);

    @Query("SELECT s FROM SemiFinishedBatchInventory s " +
           "WHERE s.productId = :productId " +
           "AND s.miniStoreId = :miniStoreId " +
           "AND s.expiryDate > :now " +
           "AND (s.availableQty - s.reservedQty) > 0 " +
           "ORDER BY s.expiryDate ASC")
    List<SemiFinishedBatchInventory> findAvailableUnreservedBatchesFifo(
            @Param("productId") Long productId,
            @Param("miniStoreId") Long miniStoreId,
            @Param("now") LocalDateTime now);

    @Query("SELECT s FROM SemiFinishedBatchInventory s " +
           "WHERE s.productId = :productId " +
           "AND s.expiryDate > :now " +
           "AND (s.availableQty - s.reservedQty) > 0 " +
           "ORDER BY s.expiryDate ASC")
    List<SemiFinishedBatchInventory> findAvailableUnreservedBatchesFifoAnyStore(
            @Param("productId") Long productId,
            @Param("now") LocalDateTime now);

    List<SemiFinishedBatchInventory> findByMiniStoreIdOrderByExpiryDateAsc(Long miniStoreId);
}
