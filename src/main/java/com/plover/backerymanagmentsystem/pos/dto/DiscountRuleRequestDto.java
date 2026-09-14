package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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
public class DiscountRuleRequestDto {
    private String name;
    private String description;
    private UUID managerId;
    private DiscountRuleType ruleType;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<String> daysOfWeek; // List of strings: MON, TUE, etc.
    private List<Long> productIds;
    private Boolean appliedToAllProducts;
    private BigDecimal maximumDiscountValue;
}
