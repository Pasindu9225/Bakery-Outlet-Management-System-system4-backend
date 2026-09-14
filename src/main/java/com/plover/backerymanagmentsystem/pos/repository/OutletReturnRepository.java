package com.plover.backerymanagmentsystem.pos.repository;

import com.plover.backerymanagmentsystem.pos.model.OutletReturn;
import com.plover.backerymanagmentsystem.pos.model.OutletReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutletReturnRepository extends JpaRepository<OutletReturn, Long> {
    List<OutletReturn> findByStatus(OutletReturnStatus status);
    List<OutletReturn> findByOutletId(Long outletId);
}
