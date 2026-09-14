package com.plover.backerymanagmentsystem.pos.repository;

import com.plover.backerymanagmentsystem.pos.model.OutletTransferRequest;
import com.plover.backerymanagmentsystem.pos.model.OutletTransferRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutletTransferRequestRepository extends JpaRepository<OutletTransferRequest, Long> {
    List<OutletTransferRequest> findBySourceOutletIdAndStatusOrderByCreatedAtDesc(Long sourceOutletId, OutletTransferRequestStatus status);
}
