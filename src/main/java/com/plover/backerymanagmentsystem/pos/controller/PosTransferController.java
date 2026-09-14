package com.plover.backerymanagmentsystem.pos.controller;

import com.plover.backerymanagmentsystem.pos.dto.ApproveTransferDto;
import com.plover.backerymanagmentsystem.pos.dto.OutletTransferResponseDto;
import com.plover.backerymanagmentsystem.pos.service.PosTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/transfers")
@RequiredArgsConstructor
public class PosTransferController {

    private final PosTransferService posTransferService;

    @GetMapping("/pending")
    public ResponseEntity<List<OutletTransferResponseDto>> getPendingTransfers(@RequestParam("outletId") Long outletId) {
        return ResponseEntity.ok(posTransferService.getPendingTransfers(outletId));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveTransfer(@PathVariable("id") Long id, @RequestBody ApproveTransferDto dto, @RequestParam("approverId") java.util.UUID approverId) {
        posTransferService.approveTransfer(id, dto.getApprovedQuantity(), approverId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectTransfer(@PathVariable("id") Long id) {
        posTransferService.rejectTransfer(id);
        return ResponseEntity.ok().build();
    }
}
