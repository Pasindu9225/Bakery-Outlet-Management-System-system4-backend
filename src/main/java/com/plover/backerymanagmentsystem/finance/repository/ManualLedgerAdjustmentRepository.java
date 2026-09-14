package com.plover.backerymanagmentsystem.finance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.finance.model.ManualLedgerAdjustment;

/**
 * Repository for {@link ManualLedgerAdjustment} entity operations.
 */
@Repository
public interface ManualLedgerAdjustmentRepository extends JpaRepository<ManualLedgerAdjustment, Long> {

    /**
     * Find all manual adjustments for a given supplier.
     *
     * @param supplierId the supplier id
     * @return list of manual adjustments
     */
    List<ManualLedgerAdjustment> findBySupplierId(Long supplierId);

    /**
     * Count adjustments whose adjustment_ref starts with the given prefix.
     * Used for generating sequence numbers (e.g. ADJ-2026-XXX).
     *
     * @param prefix the prefix (e.g. "ADJ-2026-")
     * @return count of matching adjustments
     */
    @Query("SELECT COUNT(a) FROM ManualLedgerAdjustment a WHERE a.adjustmentRef LIKE CONCAT(:prefix, '%')")
    long countByAdjustmentRefPrefix(@Param("prefix") String prefix);
}
