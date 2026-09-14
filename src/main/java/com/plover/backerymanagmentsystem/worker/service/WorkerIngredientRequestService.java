package com.plover.backerymanagmentsystem.worker.service;

import java.util.List;
import com.plover.backerymanagmentsystem.worker.dto.CreateIngredientRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestDto;

public interface WorkerIngredientRequestService {
    IngredientRequestDto create(CreateIngredientRequestDto dto);
    List<IngredientRequestDto> listMyRequests();
    IngredientRequestDto confirmReceipt(Long requestId);
    List<com.plover.backerymanagmentsystem.admin.dto.RawMaterialDto> getRawMaterials();
}
