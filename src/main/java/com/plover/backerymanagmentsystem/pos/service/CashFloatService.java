package com.plover.backerymanagmentsystem.pos.service;

import java.util.UUID;
import com.plover.backerymanagmentsystem.pos.dto.CashFloatStatusResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.OpenCashFloatRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.UpdateCashFloatRequestDto;

public interface CashFloatService {
    void openFloat(OpenCashFloatRequestDto request);
    void updateFloat(UpdateCashFloatRequestDto request);
    CashFloatStatusResponseDto getStatus(UUID cashierId);
    boolean isFloatOpen(UUID cashierId);
}
