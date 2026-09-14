package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.AllPurchaseOrdersResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.DetailedPurchaseOrderResponseDto;

public interface PurchaseOrderQueryService {

    AllPurchaseOrdersResponseDto getAllPurchaseOrders();
    
    DetailedPurchaseOrderResponseDto getAllDetailedPurchaseOrders();
}


