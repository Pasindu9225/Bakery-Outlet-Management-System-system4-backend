package com.plover.backerymanagmentsystem.worker.service;

import java.util.List;
import com.plover.backerymanagmentsystem.worker.dto.*;

public interface WorkerProductionTrackingService {
    ProductionBatchDto recordBatch(RecordBatchDto dto);
    ProductionItemProgressDto getItemProgress(Long productionPlanItemId);
    List<ProductionBatchDto> listMyBatches();
}
