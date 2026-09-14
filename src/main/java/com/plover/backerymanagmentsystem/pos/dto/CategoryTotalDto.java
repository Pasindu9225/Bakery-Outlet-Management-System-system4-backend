package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;

import com.plover.backerymanagmentsystem.pos.model.PaymentCategory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTotalDto {
    private PaymentCategory category;
    private BigDecimal totalAmount;
}
