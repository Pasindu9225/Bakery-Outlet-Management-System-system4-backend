package com.plover.backerymanagmentsystem.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.finance.model.PurchaseReturnItem;

/**
 * Repository for {@link PurchaseReturnItem} entity operations.
 */
@Repository
public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, Long> {
}
