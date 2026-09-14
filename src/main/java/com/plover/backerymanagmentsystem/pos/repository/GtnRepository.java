package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.Gtn;
import com.plover.backerymanagmentsystem.pos.model.GtnStatus;

/**
 * Repository interface for GTN entities
 */
@Repository
public interface GtnRepository extends JpaRepository<Gtn, Integer> {

    /**
     * Find all GTNs that are not in 'received' or 'over received' status
     * @return List of GTNs that can be processed
     */
    @Query("SELECT g FROM Gtn g WHERE g.status NOT IN (:excludedStatuses) ORDER BY g.date DESC")
    List<Gtn> findAllByStatusNotIn(List<GtnStatus> excludedStatuses);

    /**
     * Find all GTNs that are not in 'received' or 'over received' status with their items
     * @return List of GTNs with their items eagerly loaded
     */
    @Query("SELECT DISTINCT g FROM Gtn g " +
           "LEFT JOIN FETCH g.gtnItems gi " +
           "LEFT JOIN FETCH gi.product " +
           "WHERE g.status NOT IN (:excludedStatuses) " +
           "ORDER BY g.date DESC")
    List<Gtn> findAllByStatusNotInWithItems(List<GtnStatus> excludedStatuses);

    /**
     * Find all GTNs that are not in excluded statuses, filtered by outletId if provided.
     * @return List of GTNs with their items eagerly loaded
     */
    @Query("SELECT DISTINCT g FROM Gtn g " +
           "LEFT JOIN FETCH g.gtnItems gi " +
           "LEFT JOIN FETCH gi.product " +
           "WHERE g.status NOT IN (:excludedStatuses) " +
           "AND (:outletId IS NULL OR g.outletId = :outletId) " +
           "ORDER BY g.date DESC")
    List<Gtn> findAllByStatusNotInWithItemsAndOutlet(List<GtnStatus> excludedStatuses, Long outletId);
}