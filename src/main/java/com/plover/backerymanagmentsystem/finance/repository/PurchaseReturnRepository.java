package com.plover.backerymanagmentsystem.finance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.finance.model.PurchaseReturn;

/**
 * Repository for {@link PurchaseReturn} entity operations.
 */
@Repository
public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    /**
     * Find all purchase returns for a given supplier.
     *
     * @param supplierId the supplier id
     * @return list of purchase returns
     */
    List<PurchaseReturn> findBySupplierId(Long supplierId);

    /**
     * Count purchase returns whose return_ref starts with the given prefix.
     * Used for generating sequence numbers (e.g. RTN-2026-XXX).
     *
     * @param prefix the prefix (e.g. "RTN-2026-")
     * @return count of matching purchase returns
     */
    @Query("SELECT COUNT(r) FROM PurchaseReturn r WHERE r.returnRef LIKE CONCAT(:prefix, '%')")
    long countByReturnRefPrefix(@Param("prefix") String prefix);
}
