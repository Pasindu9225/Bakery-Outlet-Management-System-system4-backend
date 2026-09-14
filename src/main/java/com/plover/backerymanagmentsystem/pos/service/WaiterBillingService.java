package com.plover.backerymanagmentsystem.pos.service;

import java.util.List;
import com.plover.backerymanagmentsystem.admin.model.BmsAuth;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.waiter.*;

public interface WaiterBillingService {
    List<BmsAuth> getWaitersForOutlet(Long outletId);
    PosWaiterItemResponseDto addWaiterItem(AddWaiterItemRequestDto requestDto);
    WaiterBillingDetailsResponseDto getWaiterBillingDetails(String waiterId);
    CreateSaleResponseDto finishWaiterBilling(FinishWaiterBillingRequestDto requestDto);
    void removeWaiterItem(Long itemId);
    void transferWaiterItems(String fromWaiterId, String toWaiterId);
}
