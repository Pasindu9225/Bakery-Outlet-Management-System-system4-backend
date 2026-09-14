package com.plover.backerymanagmentsystem.manager.service;

import com.plover.backerymanagmentsystem.manager.dto.ActualProductionDto;
import com.plover.backerymanagmentsystem.manager.dto.ActualProductionHistoryDto;
import com.plover.backerymanagmentsystem.manager.dto.ActualProductionDistributeRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;

import java.util.List;

public interface ManagerActualProductionService {
    List<ActualProductionDto> getAllActualProductions();
    void distributeToOutlet(ActualProductionDistributeRequestDto requestDto);
    List<ActualProductionHistoryDto> getProductionHistory();
    List<ActualProductionHistoryDto> getDistributionHistory();
    List<DayProductionItemResponseDto> getOutletStock(Long outletId);
    void createOutletTransferRequest(com.plover.backerymanagmentsystem.manager.dto.OutletTransferRequestDto requestDto);
}
