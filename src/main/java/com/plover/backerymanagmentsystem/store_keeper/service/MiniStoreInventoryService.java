package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.MiniStoreDailyInventoryResponse;

public interface MiniStoreInventoryService {

    MiniStoreDailyInventoryResponse getTodayInventoryByOutlet(Long outletId);
}
