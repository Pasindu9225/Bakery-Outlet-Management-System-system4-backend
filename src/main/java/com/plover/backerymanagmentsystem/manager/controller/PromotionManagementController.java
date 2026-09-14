package com.plover.backerymanagmentsystem.manager.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.manager.dto.PromotionResponseDto;
import com.plover.backerymanagmentsystem.manager.service.PromotionManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for managing Promotions (Promo Codes) in the Manager Dashboard.
 * Restricted to View and Status Toggle only.
 */
@RestController
@RequestMapping("/api/manager/promotions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PromotionManagementController {

    private final PromotionManagementService promotionManagementService;

    @GetMapping("/all")
    public ResponseEntity<List<PromotionResponseDto>> getAllPromotions() {
        log.info("Fetching all promotions");
        List<PromotionResponseDto> response = promotionManagementService.getAllPromotions();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/toggle/{id}")
    public ResponseEntity<PromotionResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Received request to toggle promotion status with ID: {}", id);
        PromotionResponseDto response = promotionManagementService.togglePromotionStatus(id);
        return ResponseEntity.ok(response);
    }
}
