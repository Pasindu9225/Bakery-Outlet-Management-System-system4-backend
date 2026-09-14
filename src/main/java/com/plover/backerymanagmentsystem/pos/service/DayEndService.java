package com.plover.backerymanagmentsystem.pos.service;

import java.time.LocalDate;
import java.util.UUID;

import com.plover.backerymanagmentsystem.pos.dto.DayEndSummaryResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ShiftClosureRequestDto;

public interface DayEndService {
    void processDayEnd(ShiftClosureRequestDto request);
    DayEndSummaryResponseDto getDayEndSummary(LocalDate closureDate, UUID cashierId);
}
