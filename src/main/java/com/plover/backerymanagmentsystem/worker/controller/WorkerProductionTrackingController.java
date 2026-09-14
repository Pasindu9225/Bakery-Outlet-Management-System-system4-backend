package com.plover.backerymanagmentsystem.worker.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.worker.dto.ProductionBatchDto;
import com.plover.backerymanagmentsystem.worker.dto.ProductionItemProgressDto;
import com.plover.backerymanagmentsystem.worker.dto.RecordBatchDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerProductionTrackingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/worker/production-tracking")
@RequiredArgsConstructor
public class WorkerProductionTrackingController {

    private final WorkerProductionTrackingService service;

    @PostMapping("/batches")
    public ResponseEntity<ProductionBatchDto> recordBatch(@Valid @RequestBody RecordBatchDto dto) {
        return ResponseEntity.ok(service.recordBatch(dto));
    }

    @GetMapping("/items/{itemId}/progress")
    public ResponseEntity<ProductionItemProgressDto> getItemProgress(@PathVariable Long itemId) {
        return ResponseEntity.ok(service.getItemProgress(itemId));
    }

    @GetMapping("/my-batches")
    public ResponseEntity<List<ProductionBatchDto>> myBatches() {
        return ResponseEntity.ok(service.listMyBatches());
    }
}
