package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.plover.backerymanagmentsystem.pos.model.PosTableItem;

@Repository
public interface PosTableItemRepository extends JpaRepository<PosTableItem, Long> {
    List<PosTableItem> findAllByTableId(Long tableId);
    List<PosTableItem> findAllByTableIdAndIsPaid(Long tableId, Boolean isPaid);

    @Query("SELECT COUNT(ti) FROM PosTableItem ti WHERE ti.isPaid = false OR ti.isPaid IS NULL")
    long countUnpaidTableItems();

    @Query("SELECT COALESCE(SUM(t.qty), 0) FROM PosTableItem t WHERE t.dayProductionItemId = :dpiId AND t.isPaid = false")
    Integer sumUnpaidQtyByDayProductionItemId(@Param("dpiId") Integer dpiId);
}
