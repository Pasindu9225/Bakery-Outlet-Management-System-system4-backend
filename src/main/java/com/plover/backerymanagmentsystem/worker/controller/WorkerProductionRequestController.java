package com.plover.backerymanagmentsystem.worker.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.worker.dto.UpdateRequestStatusDto;
import com.plover.backerymanagmentsystem.worker.dto.WorkerProductionRequestDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerProductionRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/worker/production-requests")
@RequiredArgsConstructor
public class WorkerProductionRequestController {

    private final WorkerProductionRequestService service;

    @GetMapping
    public ResponseEntity<List<WorkerProductionRequestDto>> listMyRequests() {
        return ResponseEntity.ok(service.listMyRequests());
    }

    @PatchMapping("/{planId}/status")
    public ResponseEntity<WorkerProductionRequestDto> updateStatus(
            @PathVariable Long planId,
            @Valid @RequestBody UpdateRequestStatusDto dto) {
        return ResponseEntity.ok(service.updateStatus(planId, dto.getStatus()));
    }

    @PostMapping("/{planId}/dispatch")
    public ResponseEntity<Void> dispatchPlan(@PathVariable Long planId) {
        service.dispatchPlan(planId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/store-inventory")
    public ResponseEntity<List<com.plover.backerymanagmentsystem.manager.dto.MiniStoreItemDto>> getMyStoreInventory() {
        return ResponseEntity.ok(service.getMyStoreInventory());
    }

    @PostMapping("/clear-mini-store")
    public ResponseEntity<String> clearProductionCenterMiniStore() {
        service.clearProductionCenterMiniStore();
        return ResponseEntity.ok("Bakery and Kitchen mini stores cleared successfully.");
    }
}
