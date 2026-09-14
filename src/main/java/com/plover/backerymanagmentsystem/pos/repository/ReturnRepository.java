package com.plover.backerymanagmentsystem.pos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.Return;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Integer> {

    @Query("SELECT new com.plover.backerymanagmentsystem.pos.dto.CategoryTotalDto(pm.category, SUM(r.netRefundAmount)) " +
           "FROM Return r JOIN r.paymentMethod pm " +
           "WHERE r.createdAt >= :startOfDay AND r.createdAt < :endOfDay AND r.cashierId = :cashierId " +
           "GROUP BY pm.category")
    List<com.plover.backerymanagmentsystem.pos.dto.CategoryTotalDto> calculateRefundTotalsByCashierAndDate(
            @org.springframework.data.repository.query.Param("startOfDay") LocalDateTime startOfDay, 
            @org.springframework.data.repository.query.Param("endOfDay") LocalDateTime endOfDay, 
            @org.springframework.data.repository.query.Param("cashierId") UUID cashierId);

    List<Return> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
