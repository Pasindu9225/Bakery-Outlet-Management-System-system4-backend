package com.plover.backerymanagmentsystem.admin.controller;

import com.plover.backerymanagmentsystem.admin.dto.AdminBOMRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.AdminBOMResponseDto;
import com.plover.backerymanagmentsystem.admin.service.AdminBOMService;
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
@RequestMapping("/api/v1/admin/bom")
@RequiredArgsConstructor
@Slf4j
public class AdminBOMController {

    private final AdminBOMService adminBOMService;

    @GetMapping("/all")
    public ResponseEntity<List<AdminBOMResponseDto>> getAllBOMs() {
        log.info("REST request to get all Bill of Materials");
        List<AdminBOMResponseDto> boms = adminBOMService.getAllBOMs();
        return ResponseEntity.ok(boms);
    }

    @PostMapping("/create")
    public ResponseEntity<AdminBOMResponseDto> saveBOM(@RequestBody AdminBOMRequestDto requestDto) {
        log.info("REST request to save Bill of Materials for product: {}", requestDto.getParentProductId());
        AdminBOMResponseDto savedBOM = adminBOMService.saveBOM(requestDto);
        return ResponseEntity.ok(savedBOM);
    }

    @PutMapping("/update/{parentProductId}")
    public ResponseEntity<AdminBOMResponseDto> updateBOM(@PathVariable Long parentProductId,
            @RequestBody AdminBOMRequestDto requestDto) {
        log.info("REST request to update Bill of Materials for product: {}", parentProductId);
        AdminBOMResponseDto updatedBOM = adminBOMService.updateBOM(parentProductId, requestDto);
        return ResponseEntity.ok(updatedBOM);
    }

    @DeleteMapping("/delete/{parentProductId}")
    public ResponseEntity<String> deleteBOM(@PathVariable Long parentProductId) {
        log.info("REST request to delete Bill of Materials for product: {}", parentProductId);
        adminBOMService.deleteBOM(parentProductId);
        return ResponseEntity.ok("Bill of Materials deleted successfully");
    }
}
