package com.plover.backerymanagmentsystem.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.admin.dto.CreateOutletProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.dto.OutletProductionCenterResponse;
import com.plover.backerymanagmentsystem.admin.dto.UpdateOutletProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.service.AdminOutletProductionCenterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminOutletProductionCenterController {

    private final AdminOutletProductionCenterService service;

    @PostMapping("/outlet/{outletId}/production-centers")
    public ResponseEntity<OutletProductionCenterResponse> create(
            @PathVariable Long outletId,
            @Valid @RequestBody CreateOutletProductionCenterRequest request) {
        log.info("REST request to create MPC '{}' for outlet {}", request.getName(), outletId);
        return ResponseEntity.ok(service.create(outletId, request));
    }

    @GetMapping("/outlet/{outletId}/production-centers")
    public ResponseEntity<List<OutletProductionCenterResponse>> list(@PathVariable Long outletId) {
        log.info("REST request to list MPCs for outlet {}", outletId);
        return ResponseEntity.ok(service.listForOutlet(outletId));
    }

    @PutMapping("/outlet-production-center/{id}")
    public ResponseEntity<OutletProductionCenterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOutletProductionCenterRequest request) {
        log.info("REST request to update MPC {}", id);
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/outlet-production-center/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("REST request to delete MPC {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
