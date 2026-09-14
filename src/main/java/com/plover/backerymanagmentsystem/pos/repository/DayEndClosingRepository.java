package com.plover.backerymanagmentsystem.pos.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.DayEndClosing;

@Repository
public interface DayEndClosingRepository extends JpaRepository<DayEndClosing, Integer> {
    Optional<DayEndClosing> findByOutletIdAndClosingDate(Long outletId, LocalDate closingDate);
    boolean existsByOutletIdAndClosingDate(Long outletId, LocalDate closingDate);
}
