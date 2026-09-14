package com.plover.backerymanagmentsystem.pos.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plover.backerymanagmentsystem.pos.dto.DiscountRuleResponseDto;
import com.plover.backerymanagmentsystem.pos.model.Discount;
import com.plover.backerymanagmentsystem.pos.service.DiscountService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/v1/discounts")
@RequiredArgsConstructor
public class DiscountManagementController {

    private final DiscountService discountService;

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<DiscountRuleResponseDto> toggleStatus(@PathVariable Integer id) {
        Discount discount = discountService.toggleDiscountStatus(id);
        return ResponseEntity.ok(convertToDto(discount));
    }

    @GetMapping
    public ResponseEntity<List<DiscountRuleResponseDto>> getAllDiscounts() {
        List<Discount> discounts = discountService.getAllDiscounts();
        List<DiscountRuleResponseDto> dtos = discounts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private DiscountRuleResponseDto convertToDto(Discount d) {
        return DiscountRuleResponseDto.builder()
                .discountId(d.getDiscountId())
                .name(d.getName())
                .description(d.getDescription())
                .ruleType(d.getRuleType())
                .discountType(d.getDiscountType())
                .discountValue(d.getDiscountValue())
                .startTime(d.getStartTime())
                .endTime(d.getEndTime())
                .daysOfWeek(d.getDaysOfWeek() != null ? Arrays.asList(d.getDaysOfWeek().split(",")) : null)
                .appliedToAllProducts(d.getAppliedToAllProducts())
                .isActive(d.getIsActive())
                .applicableProducts(d.getApplicableProducts() != null ? d.getApplicableProducts().stream()
                        .map(p -> DiscountRuleResponseDto.ProductSummaryDto.builder()
                                .id(p.getId())
                                .productName(p.getProductName())
                                .productCode(p.getProductCode())
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }
}
