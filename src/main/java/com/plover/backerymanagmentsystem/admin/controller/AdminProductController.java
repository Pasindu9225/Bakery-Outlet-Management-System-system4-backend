package com.plover.backerymanagmentsystem.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.admin.dto.AdminProductDto;
import com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto;
import com.plover.backerymanagmentsystem.admin.dto.ProductionStageResponseDto;
import com.plover.backerymanagmentsystem.admin.service.AdminProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/product")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final AdminProductService adminProductService;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        log.info("REST request to get all categories");
        return ResponseEntity.ok(adminProductService.getAllCategories());
    }

    @GetMapping("/production-stages")
    public ResponseEntity<List<ProductionStageResponseDto>> getAllProductionStages() {
        log.info("REST request to get all production stages");
        return ResponseEntity.ok(adminProductService.getAllProductionStages());
    }

    @org.springframework.web.bind.annotation.PostMapping("/production-stages")
    public ResponseEntity<ProductionStageResponseDto> createProductionStage(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body) {
        String name = body.get("name");
        log.info("REST request to create new production stage: {}", name);
        return ResponseEntity.ok(adminProductService.createProductionStage(name));
    }

    @org.springframework.web.bind.annotation.PutMapping("/production-stages/{id}")
    public ResponseEntity<ProductionStageResponseDto> updateProductionStage(
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body) {
        String name = body.get("name");
        log.info("REST request to update production stage {} with name: {}", id, name);
        return ResponseEntity.ok(adminProductService.updateProductionStage(id, name));
    }

    @GetMapping("/production-centers")
    public ResponseEntity<List<com.plover.backerymanagmentsystem.admin.dto.ProductionCenterResponseDto>> getAllProductionCenters() {
        log.info("REST request to get all production centers");
        return ResponseEntity.ok(adminProductService.getAllProductionCenters());
    }

    @GetMapping("/all")
    public ResponseEntity<List<AdminProductDto>> getAllProducts() {
        log.info("REST request to get all products");
        List<AdminProductDto> products = adminProductService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @org.springframework.web.bind.annotation.PostMapping("/create")
    public ResponseEntity<AdminProductDto> createProduct(
            @org.springframework.web.bind.annotation.RequestBody com.plover.backerymanagmentsystem.admin.dto.CreateProductRequestDto request) {
        log.info("REST request to create new product: {}", request.getProductName());
        AdminProductDto newProduct = adminProductService.createProduct(request);
        return ResponseEntity.ok(newProduct);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<AdminProductDto> updateProduct(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody com.plover.backerymanagmentsystem.admin.dto.UpdateProductRequestDto request) {
        log.info("REST request to update product: {}", id);
        AdminProductDto updatedProduct = adminProductService.updateProduct(id, request);
        return ResponseEntity.ok(updatedProduct);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@org.springframework.web.bind.annotation.PathVariable Long id) {
        log.info("REST request to delete product: {}", id);
        adminProductService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
