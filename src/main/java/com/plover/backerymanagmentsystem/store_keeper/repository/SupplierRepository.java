package com.plover.backerymanagmentsystem.store_keeper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;

/**
 * Repository interface for Supplier entity operations.
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Checks if a supplier exists by ID.
     *
     * @param supplierId the supplier ID to check
     * @return true if supplier exists
     */
    boolean existsBySupplierId(Long supplierId);
}
