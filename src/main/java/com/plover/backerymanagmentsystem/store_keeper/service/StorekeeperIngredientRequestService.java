package com.plover.backerymanagmentsystem.store_keeper.service;

import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.dto.IssueIngredientRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestDto;

public interface StorekeeperIngredientRequestService {
    List<IngredientRequestDto> listPending();
    List<IngredientRequestDto> listAll();
    IngredientRequestDto issue(Long requestId, IssueIngredientRequestDto dto);
}
