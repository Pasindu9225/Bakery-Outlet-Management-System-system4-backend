package com.plover.backerymanagmentsystem.pos.service;

import java.math.BigDecimal;
import java.util.List;

import com.plover.backerymanagmentsystem.pos.dto.DiscountRuleRequestDto;
import com.plover.backerymanagmentsystem.pos.model.Discount;

public interface DiscountService {
    Discount createDiscountRule(DiscountRuleRequestDto requestDto);
    Discount updateDiscountRule(Integer id, DiscountRuleRequestDto requestDto);
    void deleteDiscountRule(Integer id);
    List<Discount> getAllActiveRules();
    List<Discount> getAllDiscounts();
    BigDecimal calculateAutoDiscount(Long productId, BigDecimal unitPrice);
    Discount findApplicableDiscount(Long productId);
    Discount toggleDiscountStatus(Integer id);
}
