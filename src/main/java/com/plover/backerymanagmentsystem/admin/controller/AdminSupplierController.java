package com.plover.backerymanagmentsystem.admin.controller;

import com.plover.backerymanagmentsystem.admin.dto.CreateSupplierRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.SupplierDetailsDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateSupplierRequestDto;
import com.plover.backerymanagmentsystem.admin.service.AdminSupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ADMIN/v1")
@RequiredArgsConstructor
@Slf4j
public class AdminSupplierController {

    private final AdminSupplierService adminSupplierService;

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierDetailsDto>> getAllSupplierDetails() {
        log.info("Admin: fetching all supplier details");
        return ResponseEntity.ok(adminSupplierService.getAllSupplierDetails());
    }

    @PostMapping("/suppliers")
    public ResponseEntity<SupplierDetailsDto> createSupplier(
            @Valid @RequestBody CreateSupplierRequestDto request) {
        log.info("Admin: creating new supplier with name: {}", request.getName());
        SupplierDetailsDto createdSupplier = adminSupplierService.createSupplier(request);
        return ResponseEntity.ok(createdSupplier);
    }

    @PutMapping("/suppliers/{id}")
    public ResponseEntity<SupplierDetailsDto> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRequestDto request) {
        log.info("Admin: updating supplier with id: {}", id);
        SupplierDetailsDto updatedSupplier = adminSupplierService.updateSupplier(id, request);
        return ResponseEntity.ok(updatedSupplier);
    }

    @DeleteMapping("/suppliers/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        log.info("Admin: deleting supplier with id: {}", id);
        adminSupplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}

