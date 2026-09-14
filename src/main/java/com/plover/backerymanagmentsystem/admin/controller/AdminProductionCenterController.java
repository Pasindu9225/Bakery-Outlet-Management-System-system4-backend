package com.plover.backerymanagmentsystem.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;

import com.plover.backerymanagmentsystem.admin.dto.AdminProductionCenterResponse;
import com.plover.backerymanagmentsystem.admin.dto.MiniStoreResponse;
import com.plover.backerymanagmentsystem.admin.service.AdminProductionCenterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/production-center")
@RequiredArgsConstructor
@Slf4j
public class AdminProductionCenterController {

    private final AdminProductionCenterService adminProductionCenterService;

    @GetMapping("/all")
    public ResponseEntity<List<AdminProductionCenterResponse>> getAllProductionCenters() {
        log.info("REST request to get all production centers");
        List<AdminProductionCenterResponse> productionCenters = adminProductionCenterService.getAllProductionCenters();
        return ResponseEntity.ok(productionCenters);
    }

    @GetMapping("/mini-stores")
    public ResponseEntity<List<MiniStoreResponse>> getAllMiniStores() {
        log.info("REST request to get all mini stores");
        List<MiniStoreResponse> miniStores = adminProductionCenterService.getAllMiniStores();
        return ResponseEntity.ok(miniStores);
    }

    @org.springframework.web.bind.annotation.PostMapping("/create")
    public ResponseEntity<AdminProductionCenterResponse> createProductionCenter(
            @org.springframework.web.bind.annotation.RequestBody com.plover.backerymanagmentsystem.admin.dto.CreateProductionCenterRequest request) {
        log.info("REST request to create new production center: {}", request.getProductionCenterName());
        AdminProductionCenterResponse newProductionCenter = adminProductionCenterService
                .createProductionCenter(request);
        return ResponseEntity.ok(newProductionCenter);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<AdminProductionCenterResponse> updateProductCenter(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody com.plover.backerymanagmentsystem.admin.dto.UpdateProductionCenterRequest request) {
        log.info("REST request to update production center: {}", id);
        AdminProductionCenterResponse updatedProductionCenter = adminProductionCenterService.updateProductionCenter(id,
                request);
        return ResponseEntity.ok(updatedProductionCenter);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductionCenter(@org.springframework.web.bind.annotation.PathVariable Long id) {
        log.info("REST request to delete production center: {}", id);
        adminProductionCenterService.deleteProductionCenter(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-type/{type}")
    public ResponseEntity<List<AdminProductionCenterResponse>> getByType(@PathVariable ProductionCenterType type) {
        log.info("REST request to get production centers by type: {}", type);
        return ResponseEntity.ok(adminProductionCenterService.getProductionCentersByType(type));
    }
}
