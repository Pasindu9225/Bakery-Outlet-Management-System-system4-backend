package com.plover.backerymanagmentsystem.worker.service;

import com.plover.backerymanagmentsystem.worker.dto.WorkerKotDto;
import java.util.List;

public interface WorkerKotService {
    List<WorkerKotDto> listMyKots();
    WorkerKotDto updateKotStatus(Long kotId, String status);
}
