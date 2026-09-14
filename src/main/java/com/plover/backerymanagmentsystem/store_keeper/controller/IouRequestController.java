package com.plover.backerymanagmentsystem.store_keeper.controller;

import com.plover.backerymanagmentsystem.store_keeper.dto.ApproveIouRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateIouRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.IouRequestResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.IouSettleRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.model.IouStatus;
import com.plover.backerymanagmentsystem.store_keeper.service.IouRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/storekeeper/iou-requests")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // Assuming standard frontend port
public class IouRequestController {

    private final IouRequestService iouRequestService;

    @PostMapping
    public ResponseEntity<IouRequestResponseDto> createIouRequest(@RequestBody CreateIouRequestDto requestDto) {
        return ResponseEntity.ok(iouRequestService.createIouRequest(requestDto));
    }

    @PutMapping("/{id}/approve/{managerId}")
    public ResponseEntity<IouRequestResponseDto> approveIouRequest(
            @PathVariable Long id, 
            @PathVariable UUID managerId,
            @RequestBody(required = false) ApproveIouRequestDto approveDto) {
        return ResponseEntity.ok(iouRequestService.approveIouRequest(id, managerId, approveDto));
    }

    @PutMapping("/{id}/settle/{storekeeperId}")
    public ResponseEntity<IouRequestResponseDto> settleIouRequest(
            @PathVariable Long id, @PathVariable UUID storekeeperId,
            @RequestBody IouSettleRequestDto settleDto) {
        return ResponseEntity.ok(iouRequestService.settleIouRequest(id, storekeeperId, settleDto));
    }

    @PutMapping("/{id}/final-approve/{managerId}")
    public ResponseEntity<IouRequestResponseDto> approveFinalSettlement(
            @PathVariable Long id, @PathVariable UUID managerId) {
        return ResponseEntity.ok(iouRequestService.approveFinalSettlement(id, managerId));
    }

    @GetMapping
    public ResponseEntity<List<IouRequestResponseDto>> getAllIouRequests(
            @RequestParam(required = false) IouStatus status) {
        if (status != null) {
            return ResponseEntity.ok(iouRequestService.getIouRequestsByStatus(status));
        }
        return ResponseEntity.ok(iouRequestService.getAllIouRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IouRequestResponseDto> getIouRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(iouRequestService.getIouRequestById(id));
    }
}
