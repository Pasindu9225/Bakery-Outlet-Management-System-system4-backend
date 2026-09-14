package com.plover.backerymanagmentsystem.manager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.plover.backerymanagmentsystem.manager.dto.PromotionRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.PromotionResponseDto;
import com.plover.backerymanagmentsystem.manager.service.PromotionManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for full CRUD management of Promotions (Promo Codes) by Admin.
 */
@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminPromotionManagementController {

    private final PromotionManagementService promotionManagementService;

    @PostMapping("/create")
    public ResponseEntity<PromotionResponseDto> createPromotion(@Valid @RequestBody PromotionRequestDto requestDto) {
        log.info("Admin request to create promotion: {}", requestDto.getPromoCode());
        PromotionResponseDto response = promotionManagementService.createPromotion(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<PromotionResponseDto> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequestDto requestDto) {
        log.info("Admin request to update promotion with ID: {}", id);
        PromotionResponseDto response = promotionManagementService.updatePromotion(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<PromotionResponseDto>> getAllPromotions() {
        log.info("Admin fetching all promotions");
        List<PromotionResponseDto> response = promotionManagementService.getAllPromotions();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        log.info("Admin request to delete promotion with ID: {}", id);
        promotionManagementService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/toggle/{id}")
    public ResponseEntity<PromotionResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Admin request to toggle promotion status with ID: {}", id);
        PromotionResponseDto response = promotionManagementService.togglePromotionStatus(id);
        return ResponseEntity.ok(response);
    }
}
