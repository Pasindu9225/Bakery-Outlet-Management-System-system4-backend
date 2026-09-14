package com.plover.backerymanagmentsystem.manager.controller;

import com.plover.backerymanagmentsystem.manager.dto.ActualProductionDto;
import com.plover.backerymanagmentsystem.manager.dto.ActualProductionHistoryDto;
import com.plover.backerymanagmentsystem.manager.dto.ActualProductionDistributeRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;
import com.plover.backerymanagmentsystem.manager.service.ManagerActualProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/actual-production")
@RequiredArgsConstructor
public class ManagerActualProductionController {

    private final ManagerActualProductionService service;

    @GetMapping
    public ResponseEntity<List<ActualProductionDto>> getAllActualProductions() {
        return ResponseEntity.ok(service.getAllActualProductions());
    }

    @PostMapping("/distribute")
    public ResponseEntity<Void> distributeToOutlet(@Valid @RequestBody ActualProductionDistributeRequestDto requestDto) {
        service.distributeToOutlet(requestDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/production")
    public ResponseEntity<List<ActualProductionHistoryDto>> getProductionHistory() {
        return ResponseEntity.ok(service.getProductionHistory());
    }

    @GetMapping("/history/distribution")
    public ResponseEntity<List<ActualProductionHistoryDto>> getDistributionHistory() {
        return ResponseEntity.ok(service.getDistributionHistory());
    }

    @GetMapping("/outlet-stock")
    public ResponseEntity<List<com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto>> getOutletStock(@RequestParam Long outletId) {
        return ResponseEntity.ok(service.getOutletStock(outletId));
    }

    @PostMapping("/outlet-transfer")
    public ResponseEntity<Void> createOutletTransferRequest(@RequestBody com.plover.backerymanagmentsystem.manager.dto.OutletTransferRequestDto requestDto) {
        service.createOutletTransferRequest(requestDto);
        return ResponseEntity.ok().build();
    }
}
