package com.plover.backerymanagmentsystem.pos.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.pos.dto.PromotionValidationResponseDto;
import com.plover.backerymanagmentsystem.pos.model.Promotion;
import com.plover.backerymanagmentsystem.pos.repository.PromotionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for POS Promotion real-time validation.
 */
@RestController
@RequestMapping("/api/pos/v1/promotions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PosPromotionController {

    private final PromotionRepository promotionRepository;

    /**
     * Endpoint to validate a promo code in real-time on the POS client.
     *
     * @param code The promo code string to validate
     * @return Promotion validation response if valid, or HTTP 400 if invalid/expired.
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validatePromotion(@RequestParam("code") String code) {
        log.info("Received request to validate promo code: {}", code);

        Optional<Promotion> promotionOpt = promotionRepository.findByPromoCodeIgnoreCase(code);

        if (promotionOpt.isEmpty()) {
            log.warn("Promo code not found: {}", code);
            return ResponseEntity.badRequest().body("Promotion Expired or Invalid");
        }

        Promotion promotion = promotionOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // Check if isActive is true and if current time is within validity window
        if (!promotion.getIsActive() || now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            log.warn("Promo code expired or inactive: {}", code);
            return ResponseEntity.badRequest().body("Promotion Expired or Invalid");
        }

        // Promo is valid, return the DTO
        PromotionValidationResponseDto responseDto = PromotionValidationResponseDto.builder()
                .id(promotion.getId())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .maximumDiscountValue(promotion.getMaximumDiscountValue())
                .build();

        log.info("Successfully validated promo code: {}", code);
        return ResponseEntity.ok(responseDto);
    }
}
