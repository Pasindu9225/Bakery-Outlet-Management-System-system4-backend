package com.plover.backerymanagmentsystem.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.admin.service.AdminOutletService;
import com.plover.backerymanagmentsystem.manager.model.Outlet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/outlet")
@RequiredArgsConstructor
@Slf4j
public class AdminOutletController {

    private final AdminOutletService adminOutletService;

    @GetMapping("/all")
    public ResponseEntity<List<Outlet>> getAllOutlets() {
        log.info("REST request to get all outlets");
        List<Outlet> outlets = adminOutletService.getAllOutlets();
        return ResponseEntity.ok(outlets);
    }

    @org.springframework.web.bind.annotation.PostMapping("/create")
    public ResponseEntity<Outlet> createOutlet(
            @org.springframework.web.bind.annotation.RequestBody com.plover.backerymanagmentsystem.admin.dto.CreateOutletRequest request) {
        log.info("REST request to create new outlet: {}", request.getName());
        Outlet outlet = adminOutletService.createOutlet(request);
        return ResponseEntity.ok(outlet);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<Outlet> updateOutlet(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody com.plover.backerymanagmentsystem.admin.dto.UpdateOutletRequest request) {
        log.info("REST request to update outlet: {}", id);
        Outlet updatedOutlet = adminOutletService.updateOutlet(id, request);
        return ResponseEntity.ok(updatedOutlet);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOutlet(@org.springframework.web.bind.annotation.PathVariable Long id) {
        log.info("REST request to delete outlet: {}", id);
        adminOutletService.deleteOutlet(id);
        return ResponseEntity.noContent().build();
    }
}
