package com.plover.backerymanagmentsystem.finance.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.finance.model.SupplierPaymentAllocation;

/**
 * Repository for {@link SupplierPaymentAllocation} entity operations.
 */
@Repository
public interface SupplierPaymentAllocationRepository extends JpaRepository<SupplierPaymentAllocation, Long> {

    /**
     * Sum the allocated amount of all payment allocations targeting a specific
     * GRN. Used to determine outstanding balances per GRN.
     *
     * @param grnId the GRN id
     * @return total allocated to the GRN, or {@code null} if no allocations
     */
    @Query("SELECT COALESCE(SUM(a.allocatedAmount), 0) FROM SupplierPaymentAllocation a WHERE a.grnId = :grnId")
    BigDecimal sumAllocatedByGrnId(@Param("grnId") Long grnId);
}
