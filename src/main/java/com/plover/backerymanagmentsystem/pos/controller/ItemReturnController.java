package com.plover.backerymanagmentsystem.pos.controller;

import com.plover.backerymanagmentsystem.pos.dto.ReturnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.ReturnResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetSaleByIdResponseDto;
import com.plover.backerymanagmentsystem.pos.service.ItemReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Item Return process in POS module
 */
@RestController
@RequestMapping("/api/pos/v1/item-return")
@RequiredArgsConstructor
@Slf4j
public class ItemReturnController {

    private final ItemReturnService itemReturnService;

    /**
     * Get all sales for the current date with full details.
     * This is used to browse sales that are eligible for item returns today.
     *
     * @return ResponseEntity containing the list of today's sales with items
     */
    @GetMapping("/today-sales")
    public ResponseEntity<List<GetSaleByIdResponseDto>> getTodaySales() {
        log.info("Received request to get today's sales with details for item return process");

        try {
            List<GetSaleByIdResponseDto> response = itemReturnService.getTodaySalesWithDetails();
            log.info("Successfully retrieved {} sales for today", response.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching today's sales for item return: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Process an item return or exchange.
     *
     * @param requestDto the return/exchange request details
     * @return ResponseEntity containing the result of the operation
     */
    @PostMapping("/process")
    public ResponseEntity<ReturnResponseDto> processReturn(@RequestBody ReturnRequestDto requestDto) {
        log.info("Received request to process {} for sale ID: {}", requestDto.getRefundType(), requestDto.getSaleId());

        try {
            ReturnResponseDto response = itemReturnService.processReturn(requestDto);
            log.info("Successfully processed return with ID: {}", response.getReturnId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing return: {}", e.getMessage(), e);
            throw e;
        }
    }
}
