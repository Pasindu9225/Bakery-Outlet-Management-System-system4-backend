package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.plover.backerymanagmentsystem.pos.model.DiscountRuleType;
import com.plover.backerymanagmentsystem.pos.model.DiscountType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRuleResponseDto {
    private Integer discountId;
    private String name;
    private String description;
    private DiscountRuleType ruleType;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<String> daysOfWeek;
    private List<ProductSummaryDto> applicableProducts;
    private Boolean appliedToAllProducts;
    private Boolean isActive;
    private BigDecimal maximumDiscountValue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummaryDto {
        private Long id;
        private String productName;
        private String productCode;
    }
}
