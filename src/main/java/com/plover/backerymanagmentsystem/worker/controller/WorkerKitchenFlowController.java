package com.plover.backerymanagmentsystem.worker.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.plover.backerymanagmentsystem.worker.dto.*;
import com.plover.backerymanagmentsystem.worker.service.WorkerKitchenFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/worker/kitchen-flow")
@RequiredArgsConstructor
public class WorkerKitchenFlowController {

    private final WorkerKitchenFlowService service;

    @PostMapping("/transfer-notes")
    public ResponseEntity<TransferNoteDto> createTransferNote(@Valid @RequestBody CreateTransferNoteDto dto) {
        return ResponseEntity.ok(service.createTransferNote(dto));
    }

    @GetMapping("/transfer-notes")
    public ResponseEntity<List<TransferNoteDto>> listTransferNotes() {
        return ResponseEntity.ok(service.listMyTransferNotes());
    }

    @PostMapping("/transfer-notes/{id}/mark-received")
    public ResponseEntity<TransferNoteDto> markReceived(@PathVariable Long id) {
        return ResponseEntity.ok(service.markTransferReceived(id));
    }

    @PostMapping("/returns")
    public ResponseEntity<KitchenReturnDto> createReturn(@Valid @RequestBody CreateKitchenReturnDto dto) {
        return ResponseEntity.ok(service.createReturn(dto));
    }

    @GetMapping("/returns")
    public ResponseEntity<List<KitchenReturnDto>> listReturns() {
        return ResponseEntity.ok(service.listMyReturns());
    }
}
