package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyDiscountSummaryDto {
    private Integer totalDiscountCount;
    private BigDecimal totalDiscountValue;
    private Double averageDiscountPercentage;
}
