package com.plover.backerymanagmentsystem.pos.service;

import com.plover.backerymanagmentsystem.pos.dto.OutletTransferResponseDto;

import java.util.List;

public interface PosTransferService {
    List<OutletTransferResponseDto> getPendingTransfers(Long outletId);
    void approveTransfer(Long requestId, Integer approvedQuantity, java.util.UUID approverId);
    void rejectTransfer(Long requestId);
}
