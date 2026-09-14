package com.plover.backerymanagmentsystem.manager.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.dto.PromotionRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.PromotionResponseDto;
import com.plover.backerymanagmentsystem.manager.service.PromotionManagementService;
import com.plover.backerymanagmentsystem.pos.model.Promotion;
import com.plover.backerymanagmentsystem.pos.repository.PromotionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionManagementServiceImpl implements PromotionManagementService {

    private final PromotionRepository promotionRepository;

    @Override
    @Transactional
    public PromotionResponseDto createPromotion(PromotionRequestDto requestDto) {
        if (promotionRepository.findByPromoCodeIgnoreCase(requestDto.getPromoCode()).isPresent()) {
            throw new IllegalArgumentException("Promotion code already exists");
        }

        validatePromotionRequest(requestDto);

        Promotion promotion = Promotion.builder()
                .promoCode(requestDto.getPromoCode())
                .description(requestDto.getDescription())
                .discountType(requestDto.getDiscountType())
                .discountValue(requestDto.getDiscountValue())
                .startDate(requestDto.getStartDate())
                .endDate(requestDto.getEndDate())
                .isActive(requestDto.getIsActive() != null ? requestDto.getIsActive() : true)
                .maximumDiscountValue(requestDto.getMaximumDiscountValue())
                .build();

        Promotion saved = promotionRepository.save(promotion);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public PromotionResponseDto updatePromotion(Long id, PromotionRequestDto requestDto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with id: " + id));

        validatePromotionRequest(requestDto);

        // Check if code is being changed to an existing one
        if (!promotion.getPromoCode().equalsIgnoreCase(requestDto.getPromoCode()) && 
            promotionRepository.findByPromoCodeIgnoreCase(requestDto.getPromoCode()).isPresent()) {
            throw new IllegalArgumentException("New promotion code already exists");
        }

        promotion.setPromoCode(requestDto.getPromoCode());
        promotion.setDescription(requestDto.getDescription());
        promotion.setDiscountType(requestDto.getDiscountType());
        promotion.setDiscountValue(requestDto.getDiscountValue());
        promotion.setStartDate(requestDto.getStartDate());
        promotion.setEndDate(requestDto.getEndDate());
        
        if (requestDto.getIsActive() != null) {
            promotion.setIsActive(requestDto.getIsActive());
        }
        
        promotion.setMaximumDiscountValue(requestDto.getMaximumDiscountValue());

        Promotion saved = promotionRepository.save(promotion);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionResponseDto> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException("Promotion not found with id: " + id);
        }
        promotionRepository.deleteById(id);
    }

    private PromotionResponseDto mapToDto(Promotion promotion) {
        return PromotionResponseDto.builder()
                .id(promotion.getId())
                .promoCode(promotion.getPromoCode())
                .description(promotion.getDescription())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .isActive(promotion.getIsActive())
                .maximumDiscountValue(promotion.getMaximumDiscountValue())
                .build();
    }

    @Override
    @Transactional
    public PromotionResponseDto togglePromotionStatus(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with id: " + id));
        promotion.setIsActive(!promotion.getIsActive());
        Promotion saved = promotionRepository.save(promotion);
        return mapToDto(saved);
    }

    private void validatePromotionRequest(PromotionRequestDto dto) {
        if (dto.getMaximumDiscountValue() != null && dto.getDiscountType() == com.plover.backerymanagmentsystem.pos.model.DiscountType.FLAT) {
            if (dto.getDiscountValue() > dto.getMaximumDiscountValue()) {
                throw new IllegalArgumentException("Discount value cannot exceed maximum discount value");
            }
        }
    }
}
