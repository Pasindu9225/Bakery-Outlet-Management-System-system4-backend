package com.plover.backerymanagmentsystem.finance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.finance.model.SupplierPayment;

/**
 * Repository for {@link SupplierPayment} entity operations.
 */
@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    /**
     * Find all payments for a given supplier.
     *
     * @param supplierId the supplier id
     * @return list of supplier payments (no eager fetch)
     */
    List<SupplierPayment> findBySupplierId(Long supplierId);

    /**
     * Count payments whose payment_ref starts with the given prefix. Used for
     * generating sequence numbers (e.g. PMT-2026-XXX).
     *
     * @param prefix the prefix (e.g. "PMT-2026-")
     * @return count of matching payments
     */
    @Query("SELECT COUNT(p) FROM SupplierPayment p WHERE p.paymentRef LIKE CONCAT(:prefix, '%')")
    long countByPaymentRefPrefix(@Param("prefix") String prefix);

    /**
     * Return all payments ordered by payment date descending, then by payment
     * id descending so newest creations appear first when dates are equal.
     */
    List<SupplierPayment> findAllByOrderByPaymentDateDescPaymentIdDesc();
}
