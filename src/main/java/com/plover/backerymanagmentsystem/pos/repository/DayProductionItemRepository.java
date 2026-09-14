package com.plover.backerymanagmentsystem.pos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;

/**
 * Repository interface for DayProductionItem entities
 */
@Repository
public interface DayProductionItemRepository extends JpaRepository<DayProductionItem, Integer> {

        @Query("SELECT dpi FROM DayProductionItem dpi "
                        + "JOIN FETCH dpi.product p "
                        + "WHERE dpi.dayProduction.productionId = :productionId")
        List<DayProductionItem> findByProductionIdWithProduct(@Param("productionId") Integer productionId);

        @Query("SELECT dpi FROM DayProductionItem dpi "
                        + "JOIN FETCH dpi.product p "
                        + "WHERE dpi.dayProduction.productionId IN :productionIds")
        List<DayProductionItem> findByProductionIdInWithProduct(@Param("productionIds") List<Integer> productionIds);

        @Query("SELECT dpi FROM DayProductionItem dpi " +
               "JOIN dpi.dayProduction dp " +
               "WHERE dpi.product.id = :productId AND dp.outletId = :outletId " +
               "ORDER BY dpi.dayProductionItemId DESC")
        List<DayProductionItem> findFirstByProduct_IdAndOutletIdOrderByDayProductionItemIdDesc(
            @Param("productId") Long productId, @Param("outletId") Long outletId);

        Optional<DayProductionItem> findFirstByProduct_IdOrderByDayProductionItemIdDesc(Long productId);

        @Query("SELECT dpi FROM DayProductionItem dpi " +
               "JOIN FETCH dpi.product p " +
               "JOIN dpi.dayProduction dp " +
               "WHERE dp.orderedDate = :date " +
               "AND (:outletId IS NULL OR dp.outletId = :outletId) " +
               "AND dpi.currentQty <= :threshold")
        List<DayProductionItem> findLowStockItems(
            @Param("date") java.time.LocalDate date, 
            @Param("threshold") Integer threshold,
            @Param("outletId") Long outletId);
}
