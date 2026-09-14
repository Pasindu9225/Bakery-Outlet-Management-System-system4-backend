package com.plover.backerymanagmentsystem.pos.service;

import com.plover.backerymanagmentsystem.pos.dto.DailyDiscountSummaryDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetAllSalesResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetSaleByIdResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.StandaloneKotRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.StandaloneKotResponseDto;

/**
 * Service interface for sale operations
 */
public interface SaleService {

    CreateSaleResponseDto createSale(CreateSaleRequestDto requestDto);

    GetAllSalesResponseDto getAllSales();

    GetSaleByIdResponseDto getSaleById(Integer saleId);

    DailyDiscountSummaryDto getDailyDiscountSummary();

    StandaloneKotResponseDto createStandaloneKot(StandaloneKotRequestDto dto);

    com.plover.backerymanagmentsystem.pos.dto.PosDashboardStatsResponseDto getDashboardStats(Long outletId);

    void updateInvoicePrintedStatus(Integer saleId, Boolean invoicePrinted);
}
