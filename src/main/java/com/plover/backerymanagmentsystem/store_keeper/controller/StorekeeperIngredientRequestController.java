package com.plover.backerymanagmentsystem.store_keeper.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.store_keeper.dto.IssueIngredientRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.service.StorekeeperIngredientRequestService;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/storekeeper/ingredient-requests")
@RequiredArgsConstructor
public class StorekeeperIngredientRequestController {

    private final StorekeeperIngredientRequestService service;

    @GetMapping("/pending")
    public ResponseEntity<List<IngredientRequestDto>> pending() {
        return ResponseEntity.ok(service.listPending());
    }

    @GetMapping
    public ResponseEntity<List<IngredientRequestDto>> all() {
        return ResponseEntity.ok(service.listAll());
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<IngredientRequestDto> issue(
            @PathVariable Long id,
            @RequestBody IssueIngredientRequestDto dto) {
        return ResponseEntity.ok(service.issue(id, dto));
    }
}
