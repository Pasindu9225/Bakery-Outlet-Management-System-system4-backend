package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.SaleItem;

/**
 * Repository interface for SaleItem entities
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Integer> {

    /**
     * Find sale items by sale ID
     */
    List<SaleItem> findBySaleId(Integer saleId);

    /**
     * Find sale items by day production item ID
     */
    List<SaleItem> findByDayProductionItemId(Integer dayProductionItemId);

    /**
     * Find sale items with free meal reasons
     */
    List<SaleItem> findByFreeMealReasonIsNotNull();
}
