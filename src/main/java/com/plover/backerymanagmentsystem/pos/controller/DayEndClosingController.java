package com.plover.backerymanagmentsystem.pos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.pos.dto.DayEndClosingRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.DayEndClosingResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;
import com.plover.backerymanagmentsystem.pos.service.DayEndClosingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/pos/day-end")
@RequiredArgsConstructor
@Slf4j
public class DayEndClosingController {

    private final DayEndClosingService dayEndClosingService;

    @GetMapping("/inventory")
    public ResponseEntity<List<DayProductionItemResponseDto>> getClosingInventory(
            @RequestParam(required = false) Long outletId) {
        log.info("API request to fetch closing inventory for outlet: {}", outletId);
        List<DayProductionItemResponseDto> inventory = dayEndClosingService.getClosingInventory(outletId);
        return ResponseEntity.ok(inventory);
    }

    @PostMapping("/submit")
    public ResponseEntity<DayEndClosingResponseDto> submitClosing(
            @RequestBody DayEndClosingRequestDto request) {
        log.info("API request to submit stock closing for outlet: {}", request.getOutletId());
        DayEndClosingResponseDto response = dayEndClosingService.submitClosing(request);
        return ResponseEntity.ok(response);
    }
}
