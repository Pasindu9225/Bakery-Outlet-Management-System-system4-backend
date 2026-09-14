package com.plover.backerymanagmentsystem.pos.dto;

import com.plover.backerymanagmentsystem.pos.model.DiscountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for promotion code validation containing the rule details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidationResponseDto {
    private Long id;
    private DiscountType discountType;
    private Double discountValue;
    private Double maximumDiscountValue; // null means no cap
}
