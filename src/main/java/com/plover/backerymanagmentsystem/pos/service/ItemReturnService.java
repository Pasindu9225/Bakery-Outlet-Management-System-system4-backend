package com.plover.backerymanagmentsystem.pos.service;

import java.util.List;
import com.plover.backerymanagmentsystem.pos.dto.GetSaleByIdResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ReturnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.ReturnResponseDto;

/**
 * Service interface for item return operations
 */
public interface ItemReturnService {

    /**
     * Get all sales for the current date with full details
     *
     * @return list of sales with their items
     */
    List<GetSaleByIdResponseDto> getTodaySalesWithDetails();

    /**
     * Process an item return or exchange transaction
     *
     * @param requestDto the return/exchange request details
     * @return response indicating success and the return ID
     */
    ReturnResponseDto processReturn(ReturnRequestDto requestDto);
}
