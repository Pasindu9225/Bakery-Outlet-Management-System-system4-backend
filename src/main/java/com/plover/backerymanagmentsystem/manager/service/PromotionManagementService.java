package com.plover.backerymanagmentsystem.manager.service;

import java.util.List;

import com.plover.backerymanagmentsystem.manager.dto.PromotionRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.PromotionResponseDto;

public interface PromotionManagementService {
    PromotionResponseDto createPromotion(PromotionRequestDto requestDto);
    PromotionResponseDto updatePromotion(Long id, PromotionRequestDto requestDto);
    List<PromotionResponseDto> getAllPromotions();
    void deletePromotion(Long id);
    PromotionResponseDto togglePromotionStatus(Long id);
}
