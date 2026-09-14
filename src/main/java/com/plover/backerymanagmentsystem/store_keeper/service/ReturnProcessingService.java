package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.ReturnMaterialsRequestDto;

public interface ReturnProcessingService {
    void processReturn(ReturnMaterialsRequestDto request);
}


