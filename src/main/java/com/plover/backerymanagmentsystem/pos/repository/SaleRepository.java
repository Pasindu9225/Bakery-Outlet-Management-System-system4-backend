package com.plover.backerymanagmentsystem.pos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.Sale;

/**
 * Repository interface for Sale entities
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, Integer> {

    /**
     * Find sales by date
     */
    List<Sale> findBySaleDate(LocalDate saleDate);

    /**
     * Find sales by cashier ID
     */
    List<Sale> findByCashierId(UUID cashierId);

    /**
     * Find sales by date and cashier ID
     */
    List<Sale> findBySaleDateAndCashierId(LocalDate saleDate, UUID cashierId);

    /**
     * Find sales by date range
     */
    List<Sale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Calculate expected totals per PaymentCategory for a specific cashier and date
     */
    @Query("SELECT new com.plover.backerymanagmentsystem.pos.dto.CategoryTotalDto(pm.category, SUM(si.price * si.qty - coalesce(si.appliedDiscount, 0.0))) " +
           "FROM Sale s JOIN s.saleItems si JOIN si.paymentMethod pm " +
           "WHERE s.saleDate = :saleDate AND s.cashierId = :cashierId " +
           "GROUP BY pm.category")
    List<com.plover.backerymanagmentsystem.pos.dto.CategoryTotalDto> calculateExpectedTotalsByCashierAndDate(
            @org.springframework.data.repository.query.Param("saleDate") LocalDate saleDate, 
            @org.springframework.data.repository.query.Param("cashierId") UUID cashierId);

    /**
     * Checks if there are any uncompleted sale items for a cashier on a specific date.
     */
    @Query("SELECT CASE WHEN COUNT(si) > 0 THEN true ELSE false END " +
           "FROM Sale s JOIN s.saleItems si " +
           "WHERE s.saleDate = :saleDate AND s.cashierId = :cashierId " +
           "AND si.itemStatus IN :statuses")
    boolean hasPendingTransactionsForCashierAndDate(
            @org.springframework.data.repository.query.Param("saleDate") LocalDate saleDate, 
            @org.springframework.data.repository.query.Param("cashierId") UUID cashierId,
            @org.springframework.data.repository.query.Param("statuses") java.util.List<com.plover.backerymanagmentsystem.pos.model.SaleItemStatus> statuses);

    List<Sale> findBySaleDateAndOutletIdOrderBySaleTimeDesc(LocalDate saleDate, Long outletId);
}
