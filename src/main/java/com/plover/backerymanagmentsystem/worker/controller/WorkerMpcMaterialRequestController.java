package com.plover.backerymanagmentsystem.worker.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.worker.dto.CreateMpcMaterialRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.MpcMaterialRequestResponseDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerMpcMaterialRequestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/worker/mpc-material-requests")
@RequiredArgsConstructor
public class WorkerMpcMaterialRequestController {

    private final WorkerMpcMaterialRequestService service;

    @PostMapping
    public ResponseEntity<MpcMaterialRequestResponseDto> createRequest(@RequestBody CreateMpcMaterialRequestDto dto) {
        return ResponseEntity.ok(service.createRequest(dto));
    }

    @GetMapping
    public ResponseEntity<List<MpcMaterialRequestResponseDto>> listRequests() {
        return ResponseEntity.ok(service.listRequests());
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<MpcMaterialRequestResponseDto> acceptMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(service.acceptMaterials(id));
    }
}
