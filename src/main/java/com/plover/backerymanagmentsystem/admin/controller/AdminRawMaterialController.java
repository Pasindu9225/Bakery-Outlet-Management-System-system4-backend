package com.plover.backerymanagmentsystem.admin.controller;

import com.plover.backerymanagmentsystem.admin.dto.BrandDto;
import com.plover.backerymanagmentsystem.admin.dto.CreateRawMaterialRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.GenericMaterialDto;
import com.plover.backerymanagmentsystem.admin.dto.RawMaterialDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateRawMaterialRequestDto;
import com.plover.backerymanagmentsystem.admin.service.AdminRawMaterialService;
import com.plover.backerymanagmentsystem.admin.service.BrandService;
import com.plover.backerymanagmentsystem.admin.service.GenericMaterialService;
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
public class AdminRawMaterialController {

    private final AdminRawMaterialService adminRawMaterialService;
    private final GenericMaterialService genericMaterialService;
    private final BrandService brandService;

    @GetMapping("/raw-materials")
    public ResponseEntity<List<RawMaterialDto>> getAllRawMaterials() {
        log.info("Admin: fetching all raw materials");
        List<RawMaterialDto> list = adminRawMaterialService.getAllRawMaterials();
        if (list != null) {
            for (RawMaterialDto d : list) {
                log.info("CONTROLLER DTO - ID: {}, materialName: {}, genericMaterialName: {}, brand: {}, category: {}", 
                    d.getId(), d.getMaterialName(), d.getGenericMaterialName(), d.getBrand(), d.getCategory());
            }
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/raw-materials")
    public ResponseEntity<?> createRawMaterial(
            @Valid @RequestBody CreateRawMaterialRequestDto request) {
        try {
            log.info("Admin: creating new raw material with code: {} and name: {}", request.getMaterialCode(), request.getName());
            RawMaterialDto createdRawMaterial = adminRawMaterialService.createRawMaterial(request);
            return ResponseEntity.ok(createdRawMaterial);
        } catch (Exception e) {
            log.error("Controller Error creating raw material: ", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/raw-materials/{id}")
    public ResponseEntity<RawMaterialDto> updateRawMaterial(
            @PathVariable Long id,
            @RequestBody UpdateRawMaterialRequestDto request) {
        log.info("Admin: updating raw material with id: {}", id);
        RawMaterialDto updatedRawMaterial = adminRawMaterialService.updateRawMaterial(id, request);
        return ResponseEntity.ok(updatedRawMaterial);
    }

    @DeleteMapping("/raw-materials/{id}")
    public ResponseEntity<Void> deleteRawMaterial(@PathVariable Long id) {
        log.info("Admin: deleting raw material with id: {}", id);
        adminRawMaterialService.deleteRawMaterial(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/raw-materials/categories")
    public ResponseEntity<List<com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto>> getAllCategories() {
        log.info("Admin: fetching all raw material categories");
        return ResponseEntity.ok(adminRawMaterialService.getAllCategories());
    }

    @GetMapping("/raw-materials/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        log.info("Admin: fetching all raw material brands");
        return ResponseEntity.ok(adminRawMaterialService.getAllBrands());
    }

    // New Hierarchical Endpoints
    @GetMapping("/generic-materials")
    public ResponseEntity<List<GenericMaterialDto>> getAllGenericMaterials() {
        log.info("Admin: fetching all generic materials");
        return ResponseEntity.ok(genericMaterialService.getAllGenericMaterials());
    }

    @GetMapping("/generic-materials/subcategory/{subcategory}")
    public ResponseEntity<List<GenericMaterialDto>> getGenericsBySubcategory(@PathVariable String subcategory) {
        log.info("Admin: fetching generics for subcategory: {}", subcategory);
        return ResponseEntity.ok(genericMaterialService.getBySubcategory(subcategory));
    }

    @GetMapping("/generic-materials/{id}/brands")
    public ResponseEntity<List<BrandDto>> getBrandsByGeneric(@PathVariable Long id) {
        log.info("Admin: fetching brands for generic material id: {}", id);
        return ResponseEntity.ok(brandService.getBrandsByGenericMaterial(id));
    }

    @PostMapping("/generic-materials")
    public ResponseEntity<GenericMaterialDto> createGenericMaterial(@Valid @RequestBody GenericMaterialDto dto) {
        log.info("Admin: creating generic material: {}", dto.getName());
        return ResponseEntity.ok(genericMaterialService.createGenericMaterial(dto));
    }

    @PostMapping("/brands")
    public ResponseEntity<BrandDto> createBrand(@Valid @RequestBody BrandDto dto) {
        log.info("Admin: creating brand: {} for generic: {}", dto.getName(), dto.getGenericMaterialId());
        return ResponseEntity.ok(brandService.createBrand(dto));
    }
    @PostMapping("/raw-materials/categories")
    public ResponseEntity<com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto> createCategory(@RequestBody java.util.Map<String, String> body) {
        String name = body.get("name");
        log.info("Admin: creating new category: {}", name);
        return ResponseEntity.ok(adminRawMaterialService.createCategory(name));
    }

    @PutMapping("/raw-materials/categories/{id}")
    public ResponseEntity<com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto> updateCategory(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> body) {
        String name = body.get("name");
        log.info("Admin: updating category {} with name: {}", id, name);
        return ResponseEntity.ok(adminRawMaterialService.updateCategory(id, name));
    }

    @PutMapping("/generic-materials/{id}")
    public ResponseEntity<GenericMaterialDto> updateGenericMaterial(
            @PathVariable Long id,
            @Valid @RequestBody GenericMaterialDto dto) {
        log.info("Admin: updating generic material: {}", id);
        return ResponseEntity.ok(genericMaterialService.updateGenericMaterial(id, dto));
    }

    @PutMapping("/brands/{id}")
    public ResponseEntity<BrandDto> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandDto dto) {
        log.info("Admin: updating brand: {}", id);
        return ResponseEntity.ok(brandService.updateBrand(id, dto));
    }
}
