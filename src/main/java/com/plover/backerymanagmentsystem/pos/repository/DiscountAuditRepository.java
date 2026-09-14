package com.plover.backerymanagmentsystem.pos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.DiscountAudit;

@Repository
public interface DiscountAuditRepository extends JpaRepository<DiscountAudit, Long> {
    
    @Query("SELECT d FROM DiscountAudit d WHERE d.createdAt >= :startOfDay AND d.createdAt <= :endOfDay")
    List<DiscountAudit> findByCreatedAtBetween(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
