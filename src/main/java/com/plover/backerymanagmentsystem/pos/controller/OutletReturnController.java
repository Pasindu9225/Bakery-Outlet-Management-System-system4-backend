package com.plover.backerymanagmentsystem.pos.controller;

import com.plover.backerymanagmentsystem.pos.dto.OutletReturnDetailDto;
import com.plover.backerymanagmentsystem.pos.dto.OutletReturnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.OutletReturnResponseDto;
import com.plover.backerymanagmentsystem.pos.model.OutletReturnStatus;
import com.plover.backerymanagmentsystem.pos.service.OutletReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pos/v1/outlet-returns")
@RequiredArgsConstructor
@Slf4j
public class OutletReturnController {

    private final OutletReturnService outletReturnService;

    @PostMapping
    public ResponseEntity<OutletReturnResponseDto> initiateReturn(@RequestBody OutletReturnRequestDto requestDto) {
        log.info("Received request to initiate outlet return");
        return ResponseEntity.ok(outletReturnService.initiateReturn(requestDto));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OutletReturnDetailDto>> getReturnsByStatus(@PathVariable OutletReturnStatus status) {
        log.info("Received request to get outlet returns with status: {}", status);
        return ResponseEntity.ok(outletReturnService.getReturnsByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutletReturnDetailDto> getReturnDetails(@PathVariable Long id) {
        log.info("Received request to get outlet return details for ID: {}", id);
        return ResponseEntity.ok(outletReturnService.getReturnDetails(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<OutletReturnResponseDto> approveReturn(
            @PathVariable Long id, 
            @RequestParam UUID approverId) {
        log.info("Received request to approve outlet return ID: {}", id);
        return ResponseEntity.ok(outletReturnService.approveReturn(id, approverId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OutletReturnResponseDto> rejectReturn(
            @PathVariable Long id, 
            @RequestParam UUID rejecterId) {
        log.info("Received request to reject outlet return ID: {}", id);
        return ResponseEntity.ok(outletReturnService.rejectReturn(id, rejecterId));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<OutletReturnResponseDto> receiveReturn(
            @PathVariable Long id, 
            @RequestParam UUID receiverId) {
        log.info("Received request to receive outlet return ID: {}", id);
        return ResponseEntity.ok(outletReturnService.receiveReturn(id, receiverId));
    }
}
