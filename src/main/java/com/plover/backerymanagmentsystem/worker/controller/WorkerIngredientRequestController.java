package com.plover.backerymanagmentsystem.worker.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.worker.dto.CreateIngredientRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerIngredientRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/worker/ingredient-requests")
@RequiredArgsConstructor
public class WorkerIngredientRequestController {

    private final WorkerIngredientRequestService service;

    @PostMapping
    public ResponseEntity<IngredientRequestDto> create(@Valid @RequestBody CreateIngredientRequestDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<IngredientRequestDto>> list() {
        return ResponseEntity.ok(service.listMyRequests());
    }

    @GetMapping("/raw-materials")
    public ResponseEntity<List<com.plover.backerymanagmentsystem.admin.dto.RawMaterialDto>> getRawMaterials() {
        return ResponseEntity.ok(service.getRawMaterials());
    }

    @PostMapping("/{id}/confirm-receipt")
    public ResponseEntity<IngredientRequestDto> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirmReceipt(id));
    }
}
