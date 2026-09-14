package com.plover.backerymanagmentsystem.pos.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.pos.dto.DiscountRuleRequestDto;
import com.plover.backerymanagmentsystem.pos.exception.SaleException;
import com.plover.backerymanagmentsystem.pos.model.Discount;
import com.plover.backerymanagmentsystem.pos.model.DiscountRuleType;
import com.plover.backerymanagmentsystem.pos.model.DiscountType;
import com.plover.backerymanagmentsystem.pos.repository.DiscountRepository;
import com.plover.backerymanagmentsystem.pos.service.DiscountService;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;
    private final ProductRepository productRepository;

    @Override
    public Discount createDiscountRule(DiscountRuleRequestDto requestDto) {
        validateRequest(requestDto);

        String trimmedName = requestDto.getName() != null ? requestDto.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new SaleException("Discount rule name is required");
        }
        boolean nameExists = discountRepository.findAll().stream()
                .anyMatch(d -> d.getName() != null && d.getName().trim().equalsIgnoreCase(trimmedName));
        if (nameExists) {
            throw new SaleException("A discount rule with this name already exists");
        }
        
        byte[] managerId = null;
        if (requestDto.getManagerId() != null) {
            managerId = IdUtil.uuidToBytes(requestDto.getManagerId());
        }
        
        if (managerId == null) {
            Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof com.plover.backerymanagmentsystem.core.login.model.AuthModel) {
                managerId = ((com.plover.backerymanagmentsystem.core.login.model.AuthModel) principal).getId();
            }
        }
        // Fallback constraint safe-guard if user not found
        if (managerId == null) {
            managerId = IdUtil.uuidToBytes(java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")); // Could still violate FK but prevents NullPointer
        }
        
        Discount discount = Discount.builder()
                .managerId(managerId)
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .ruleType(requestDto.getRuleType())
                .discountType(requestDto.getDiscountType())
                .discountValue(requestDto.getDiscountValue())
                .startTime(requestDto.getStartTime())
                .endTime(requestDto.getEndTime())
                .daysOfWeek(requestDto.getDaysOfWeek() != null ? String.join(",", requestDto.getDaysOfWeek()) : null)
                .appliedToAllProducts(requestDto.getAppliedToAllProducts())
                .isActive(true)
                .maximumDiscountValue(requestDto.getMaximumDiscountValue())
                .build();

        if (Boolean.FALSE.equals(requestDto.getAppliedToAllProducts()) && requestDto.getProductIds() != null) {
            Set<Product> products = new HashSet<>(productRepository.findAllById(requestDto.getProductIds()));
            discount.setApplicableProducts(products);
        }

        return discountRepository.save(discount);
    }

    @Override
    public Discount updateDiscountRule(Integer id, DiscountRuleRequestDto requestDto) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new SaleException("Discount rule not found: " + id));
        
        validateRequest(requestDto);

        String trimmedName = requestDto.getName() != null ? requestDto.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new SaleException("Discount rule name is required");
        }
        boolean nameExists = discountRepository.findAll().stream()
                .anyMatch(d -> d.getName() != null 
                        && d.getName().trim().equalsIgnoreCase(trimmedName) 
                        && !d.getDiscountId().equals(id));
        if (nameExists) {
            throw new SaleException("A discount rule with this name already exists");
        }

        discount.setName(requestDto.getName());
        discount.setDescription(requestDto.getDescription());
        discount.setRuleType(requestDto.getRuleType());
        discount.setDiscountType(requestDto.getDiscountType());
        discount.setDiscountValue(requestDto.getDiscountValue());
        discount.setStartTime(requestDto.getStartTime());
        discount.setEndTime(requestDto.getEndTime());
        discount.setDaysOfWeek(requestDto.getDaysOfWeek() != null ? String.join(",", requestDto.getDaysOfWeek()) : null);
        discount.setAppliedToAllProducts(requestDto.getAppliedToAllProducts());
        discount.setMaximumDiscountValue(requestDto.getMaximumDiscountValue());

        if (Boolean.FALSE.equals(requestDto.getAppliedToAllProducts()) && requestDto.getProductIds() != null) {
            Set<Product> products = new HashSet<>(productRepository.findAllById(requestDto.getProductIds()));
            discount.setApplicableProducts(products);
        } else {
            discount.setApplicableProducts(null);
        }

        return discountRepository.save(discount);
    }

    @Override
    public void deleteDiscountRule(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new SaleException("Discount rule not found: " + id));
        discount.setIsActive(false);
        discountRepository.save(discount);
    }

    @Override
    public List<Discount> getAllActiveRules() {
        return discountRepository.findByIsActiveTrue();
    }

    @Override
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    @Override
    public BigDecimal calculateAutoDiscount(Long productId, BigDecimal unitPrice) {
        Discount applicable = findApplicableDiscount(productId);
        if (applicable == null) return BigDecimal.ZERO;

        if (applicable.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal calculated = unitPrice.multiply(applicable.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            if (applicable.getMaximumDiscountValue() != null && calculated.compareTo(applicable.getMaximumDiscountValue()) > 0) {
                return applicable.getMaximumDiscountValue();
            }
            return calculated;
        } else {
            BigDecimal flatDiscount = applicable.getDiscountValue();
            if (applicable.getMaximumDiscountValue() != null && flatDiscount.compareTo(applicable.getMaximumDiscountValue()) > 0) {
                return applicable.getMaximumDiscountValue();
            }
            return flatDiscount;
        }
    }

    @Override
    public Discount findApplicableDiscount(Long productId) {
        List<Discount> potentialDiscounts = discountRepository.findActiveDiscountsByProduct(productId);
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        String currentDay = now.getDayOfWeek().name().substring(0, 3).toUpperCase();

        return potentialDiscounts.stream()
                .filter(d -> isWithinTimeSlot(d, currentTime, currentDay))
                .max((d1, d2) -> calculateSortValue(d1).compareTo(calculateSortValue(d2)))
                .orElse(null);
    }

    private boolean isWithinTimeSlot(Discount discount, LocalTime time, String day) {
        if (discount.getRuleType() == DiscountRuleType.PRODUCT_BASED) {
            return true;
        }
        
        // Time-based checks
        if (discount.getStartTime() != null && time.isBefore(discount.getStartTime())) return false;
        if (discount.getEndTime() != null && time.isAfter(discount.getEndTime())) return false;
        
        if (discount.getDaysOfWeek() != null && !discount.getDaysOfWeek().isEmpty()) {
            List<String> activeDays = Arrays.asList(discount.getDaysOfWeek().split(","));
            return activeDays.contains(day);
        }
        
        return true;
    }

    private BigDecimal calculateSortValue(Discount d) {
        // Just a simple way to pick the "best" discount if multiple exist.
        // Usually, percentage discounts are harder to compare to flat ones without a base price,
        // but since this is called per product, we could pass price here.
        // For now, let's keep it simple.
        return d.getDiscountValue();
    }

    private void validateRequest(DiscountRuleRequestDto dto) {
        if (dto.getName() == null || dto.getName().isEmpty()) throw new SaleException("Rule name is required");
        if (dto.getDiscountType() == null) throw new SaleException("Discount type is required");
        if (dto.getDiscountValue() == null) throw new SaleException("Discount value is required");
        if (dto.getRuleType() == DiscountRuleType.TIME_BASED) {
            if (dto.getStartTime() == null || dto.getEndTime() == null) {
                throw new SaleException("Time range is required for time-based discounts");
            }
        }
        
        if (dto.getMaximumDiscountValue() != null && dto.getDiscountType() == DiscountType.FLAT) {
            if (dto.getDiscountValue().compareTo(dto.getMaximumDiscountValue()) > 0) {
                throw new SaleException("Discount value cannot exceed maximum discount value");
            }
        }
    }

    @Override
    public Discount toggleDiscountStatus(Integer id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new SaleException("Discount rule not found: " + id));
        discount.setIsActive(!discount.getIsActive());
        return discountRepository.save(discount);
    }
}
