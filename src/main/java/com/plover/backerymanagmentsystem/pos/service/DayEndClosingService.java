package com.plover.backerymanagmentsystem.pos.service;

import java.util.List;

import com.plover.backerymanagmentsystem.pos.dto.DayEndClosingRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.DayEndClosingResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;

public interface DayEndClosingService {
    List<DayProductionItemResponseDto> getClosingInventory(Long outletId);
    DayEndClosingResponseDto submitClosing(DayEndClosingRequestDto request);
}
