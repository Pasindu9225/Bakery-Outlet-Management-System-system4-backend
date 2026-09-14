package com.plover.backerymanagmentsystem.manager.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.worker.dto.MpcMaterialRequestResponseDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerMpcMaterialRequestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/manager/mpc-material-requests")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ManagerMpcMaterialRequestController {

    private final WorkerMpcMaterialRequestService mpcMaterialRequestService;

    @GetMapping
    public ResponseEntity<List<MpcMaterialRequestResponseDto>> getAllMpcRequests() {
        log.info("Manager fetching all MPC material requests");
        return ResponseEntity.ok(mpcMaterialRequestService.listRequests());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<MpcMaterialRequestResponseDto> approveRequestPost(@PathVariable Long id) {
        log.info("Manager approving MPC material request ID: {}", id);
        return ResponseEntity.ok(mpcMaterialRequestService.approveRequest(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<MpcMaterialRequestResponseDto> approveRequestPut(@PathVariable Long id) {
        log.info("Manager approving MPC material request ID: {}", id);
        return ResponseEntity.ok(mpcMaterialRequestService.approveRequest(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<MpcMaterialRequestResponseDto> rejectRequestPost(@PathVariable Long id) {
        log.info("Manager rejecting MPC material request ID: {}", id);
        return ResponseEntity.ok(mpcMaterialRequestService.rejectRequest(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<MpcMaterialRequestResponseDto> rejectRequestPut(@PathVariable Long id) {
        log.info("Manager rejecting MPC material request ID: {}", id);
        return ResponseEntity.ok(mpcMaterialRequestService.rejectRequest(id));
    }
}
