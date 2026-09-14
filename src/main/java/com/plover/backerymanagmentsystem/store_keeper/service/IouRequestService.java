package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.*;
import com.plover.backerymanagmentsystem.store_keeper.model.IouStatus;

import java.util.List;
import java.util.UUID;

public interface IouRequestService {
    IouRequestResponseDto createIouRequest(CreateIouRequestDto requestDto);
    IouRequestResponseDto approveIouRequest(Long iouRequestId, UUID managerId);
    IouRequestResponseDto approveIouRequest(Long iouRequestId, UUID managerId, ApproveIouRequestDto approveDto);
    IouRequestResponseDto settleIouRequest(Long iouRequestId, UUID storekeeperId, IouSettleRequestDto settleDto);
    IouRequestResponseDto approveFinalSettlement(Long iouRequestId, UUID managerId);
    
    List<IouRequestResponseDto> getAllIouRequests();
    List<IouRequestResponseDto> getIouRequestsByStatus(IouStatus status);
    IouRequestResponseDto getIouRequestById(Long id);
}
