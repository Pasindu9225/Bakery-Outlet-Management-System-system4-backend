package com.plover.backerymanagmentsystem.pos.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.CashFloat;

@Repository
public interface CashFloatRepository extends JpaRepository<CashFloat, Long> {
    Optional<CashFloat> findByCashierIdAndFloatDate(UUID cashierId, LocalDate floatDate);
    Optional<CashFloat> findByCashierIdAndFloatDateAndIsClosed(UUID cashierId, LocalDate floatDate, Boolean isClosed);
}
