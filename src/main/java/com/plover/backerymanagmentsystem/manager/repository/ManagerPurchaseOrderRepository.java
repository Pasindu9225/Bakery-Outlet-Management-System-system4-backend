package com.plover.backerymanagmentsystem.manager.repository;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for PurchaseOrder entity operations in Manager module.
 */
@Repository("managerPurchaseOrderRepository")
public interface ManagerPurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    
    // Additional custom query methods can be added here if needed
}
