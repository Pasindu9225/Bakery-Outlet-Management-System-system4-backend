package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.plover.backerymanagmentsystem.pos.model.PosWaiterItem;

@Repository
public interface PosWaiterItemRepository extends JpaRepository<PosWaiterItem, Long> {

    List<PosWaiterItem> findByWaiterIdAndIsPaidFalse(byte[] waiterId);

    @Query("SELECT COUNT(wi) FROM PosWaiterItem wi WHERE wi.isPaid = false OR wi.isPaid IS NULL")
    long countUnpaidWaiterItems();

    @Query("SELECT COUNT(wi) FROM PosWaiterItem wi " +
           "JOIN DayProductionItem dpi ON wi.dayProductionItemId = dpi.dayProductionItemId " +
           "JOIN dpi.dayProduction dp " +
           "WHERE wi.isPaid = false AND dp.outletId = :outletId AND dp.orderedDate = :orderedDate")
    long countUnpaidWaiterItemsByOutletAndDate(
            @Param("outletId") Long outletId,
            @Param("orderedDate") LocalDate orderedDate);

    @Query("SELECT COALESCE(SUM(w.qty), 0) FROM PosWaiterItem w WHERE w.dayProductionItemId = :dpiId AND w.isPaid = false")
    Integer sumUnpaidQtyByDayProductionItemId(@Param("dpiId") Integer dpiId);
}
