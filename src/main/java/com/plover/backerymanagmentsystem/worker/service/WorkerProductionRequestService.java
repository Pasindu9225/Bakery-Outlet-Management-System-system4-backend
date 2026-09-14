package com.plover.backerymanagmentsystem.worker.service;

import java.util.List;

import com.plover.backerymanagmentsystem.manager.dto.MiniStoreItemDto;
import com.plover.backerymanagmentsystem.worker.dto.WorkerProductionRequestDto;

public interface WorkerProductionRequestService {
    List<WorkerProductionRequestDto> listMyRequests();
    WorkerProductionRequestDto updateStatus(Long planId, String newStatus);
    void dispatchPlan(Long planId);
    List<MiniStoreItemDto> getMyStoreInventory();
    void clearProductionCenterMiniStore();
}
