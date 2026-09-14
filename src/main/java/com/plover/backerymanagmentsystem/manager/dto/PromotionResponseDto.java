package com.plover.backerymanagmentsystem.manager.dto;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.pos.model.DiscountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponseDto {
    private Long id;
    private String promoCode;
    private String description;
    private DiscountType discountType;
    private Double discountValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Double maximumDiscountValue;
}
