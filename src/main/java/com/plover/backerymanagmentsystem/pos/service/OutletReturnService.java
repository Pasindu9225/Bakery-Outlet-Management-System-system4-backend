package com.plover.backerymanagmentsystem.pos.service;

import com.plover.backerymanagmentsystem.pos.dto.OutletReturnDetailDto;
import com.plover.backerymanagmentsystem.pos.dto.OutletReturnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.OutletReturnResponseDto;
import com.plover.backerymanagmentsystem.pos.model.OutletReturnStatus;

import java.util.List;
import java.util.UUID;

public interface OutletReturnService {
    OutletReturnResponseDto initiateReturn(OutletReturnRequestDto requestDto);
    List<OutletReturnDetailDto> getReturnsByStatus(OutletReturnStatus status);
    OutletReturnResponseDto approveReturn(Long returnId, UUID approverId);
    OutletReturnResponseDto rejectReturn(Long returnId, UUID rejecterId);
    OutletReturnResponseDto receiveReturn(Long returnId, UUID receiverId);
    OutletReturnDetailDto getReturnDetails(Long returnId);
}
