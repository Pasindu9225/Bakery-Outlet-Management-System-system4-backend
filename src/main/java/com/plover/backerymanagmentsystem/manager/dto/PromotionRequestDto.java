package com.plover.backerymanagmentsystem.manager.dto;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.pos.model.DiscountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequestDto {

    @NotBlank(message = "Promo code is required")
    private String promoCode;

    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    private Double discountValue;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private Boolean isActive;

    private Double maximumDiscountValue;
}
