package com.plover.backerymanagmentsystem.manager.service;

import com.plover.backerymanagmentsystem.manager.dto.MiniStoreDto;
import com.plover.backerymanagmentsystem.manager.dto.MiniStoreItemDto;
import com.plover.backerymanagmentsystem.manager.dto.MiniStoreUpdateRequestDto;

import java.util.List;

public interface MiniStoreService {
    
    List<MiniStoreDto> getAllMiniStores();
    
    List<MiniStoreItemDto> getMiniStoreItems(Integer miniStoreId);
    
    void updateMiniStoreItems(Integer miniStoreId, MiniStoreUpdateRequestDto updateRequest);
}
