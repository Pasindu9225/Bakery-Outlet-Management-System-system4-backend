package com.plover.backerymanagmentsystem.pos.service;

import java.util.List;

import com.plover.backerymanagmentsystem.pos.dto.AddTableItemRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreatePosTableRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.FinishTableBillingRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.PosTableItemResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.PosTableResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.TableBillingDetailsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ProductionCenterResponseDto;

public interface PosTableService {
    List<PosTableResponseDto> getAllTables();
    PosTableResponseDto createTable(CreatePosTableRequestDto requestDto);
    PosTableItemResponseDto addTableItem(AddTableItemRequestDto requestDto);
    List<PosTableItemResponseDto> getUnpaidItemsByTable(Long tableId);
    TableBillingDetailsResponseDto getTableBillingDetails(Long tableId);
    CreateSaleResponseDto finishTableBilling(FinishTableBillingRequestDto requestDto);

    void transferTable(Long sourceTableId, Long targetTableId);

    PosTableItemResponseDto generateKOT(Long tableItemId, Long productionCenterId);

    void cancelKOT(Long tableItemId, String verificationCode);
    void updateTableStatus(Long tableId, String status);
    List<ProductionCenterResponseDto> getAllProductionCenters();

    PosTableItemResponseDto updateTableItem(Long tableItemId, Integer qty, String instructions);
    void removeTableItem(Long tableItemId);
}
