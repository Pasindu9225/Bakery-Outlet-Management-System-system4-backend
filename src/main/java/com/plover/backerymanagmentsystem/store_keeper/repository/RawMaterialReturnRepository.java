package com.plover.backerymanagmentsystem.store_keeper.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturn;

/**
 * Repository interface for RawMaterialReturn entity operations.
 */
@Repository
public interface RawMaterialReturnRepository extends JpaRepository<RawMaterialReturn, Long> {

    /**
     * Finds all returns for a specific supplier.
     *
     * @param supplierId the supplier ID
     * @return list of raw material returns
     */
    List<RawMaterialReturn> findBySupplierId(Long supplierId);

    /**
     * Finds all returns within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of raw material returns
     */
    List<RawMaterialReturn> findByReturnDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Finds returns for a specific supplier within a date range.
     *
     * @param supplierId the supplier ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of raw material returns
     */
    @Query("SELECT r FROM RawMaterialReturn r WHERE r.supplierId = :supplierId "
            + "AND r.returnDate BETWEEN :startDate AND :endDate "
            + "ORDER BY r.returnDate DESC, r.createdAt DESC")
    List<RawMaterialReturn> findBySupplierIdAndReturnDateBetween(
            @Param("supplierId") Long supplierId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Checks if a return exists for the given supplier and date.
     *
     * @param supplierId the supplier ID
     * @param returnDate the return date
     * @return true if exists, false otherwise
     */
    boolean existsBySupplierIdAndReturnDate(Long supplierId, LocalDate returnDate);
}
