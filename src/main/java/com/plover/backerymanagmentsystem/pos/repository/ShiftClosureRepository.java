package com.plover.backerymanagmentsystem.pos.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.plover.backerymanagmentsystem.pos.model.ShiftClosure;

@Repository
public interface ShiftClosureRepository extends JpaRepository<ShiftClosure, Integer> {
    java.util.List<ShiftClosure> findByClosureDateAndOutletId(LocalDate closureDate, Integer outletId);

    Optional<ShiftClosure> findByClosureDateAndOutletIdAndCashierId(LocalDate closureDate, Integer outletId, java.util.UUID cashierId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM ShiftClosure s WHERE s.closureDate = :closureDate AND s.outletId = :outletId AND s.isLocked = true")
    Optional<ShiftClosure> findLockedDayEnd(@org.springframework.data.repository.query.Param("closureDate") LocalDate closureDate, @org.springframework.data.repository.query.Param("outletId") Integer outletId);
}
