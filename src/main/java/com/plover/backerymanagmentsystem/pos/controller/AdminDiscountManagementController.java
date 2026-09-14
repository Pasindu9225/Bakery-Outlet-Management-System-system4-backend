package com.plover.backerymanagmentsystem.pos.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.plover.backerymanagmentsystem.pos.dto.DiscountRuleRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.DiscountRuleResponseDto;
import com.plover.backerymanagmentsystem.pos.model.Discount;
import com.plover.backerymanagmentsystem.pos.service.DiscountService;

import lombok.RequiredArgsConstructor;

/**
 * Controller for full CRUD management of Discount Rules by Admin.
 */
@RestController
@RequestMapping("/api/v1/admin/discounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminDiscountManagementController {

    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<DiscountRuleResponseDto> createRule(@RequestBody DiscountRuleRequestDto requestDto) {
        Discount discount = discountService.createDiscountRule(requestDto);
        return ResponseEntity.ok(convertToDto(discount));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountRuleResponseDto> updateRule(@PathVariable Integer id, @RequestBody DiscountRuleRequestDto requestDto) {
        Discount discount = discountService.updateDiscountRule(id, requestDto);
        return ResponseEntity.ok(convertToDto(discount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Integer id) {
        discountService.deleteDiscountRule(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<DiscountRuleResponseDto> toggleStatus(@PathVariable Integer id) {
        Discount discount = discountService.toggleDiscountStatus(id);
        return ResponseEntity.ok(convertToDto(discount));
    }

    @GetMapping
    public ResponseEntity<List<DiscountRuleResponseDto>> getAllRules() {
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
                .maximumDiscountValue(d.getMaximumDiscountValue())
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
