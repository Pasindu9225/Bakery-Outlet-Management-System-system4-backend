package com.plover.backerymanagmentsystem.worker.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.worker.dto.UpdateRequestStatusDto;
import com.plover.backerymanagmentsystem.worker.dto.WorkerKotDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerKotService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/worker/kots")
@RequiredArgsConstructor
public class WorkerKotController {

    private final WorkerKotService service;

    @GetMapping
    public ResponseEntity<List<WorkerKotDto>> listMyKots() {
        return ResponseEntity.ok(service.listMyKots());
    }

    @PatchMapping("/{kotId}/status")
    public ResponseEntity<WorkerKotDto> updateStatus(
            @PathVariable Long kotId,
            @RequestBody UpdateRequestStatusDto dto) {
        return ResponseEntity.ok(service.updateKotStatus(kotId, dto.getStatus()));
    }
}
