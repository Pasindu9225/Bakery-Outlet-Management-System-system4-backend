package com.plover.backerymanagmentsystem.manager.service;

import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.ProductionPlanResponseDto;
import com.plover.backerymanagmentsystem.manager.dto.RawMaterialRequirementDto;
import com.plover.backerymanagmentsystem.manager.dto.EnhancedProductionPlanResponseDto;

import java.util.List;

public interface ProductionPlanningService {
    
    ProductionPlanResponseDto createProductionPlan(ProductionPlanRequestDto requestDto);
    
    EnhancedProductionPlanResponseDto createProductionPlanWithBom(ProductionPlanRequestDto requestDto);
    
    ProductionPlanResponseDto getProductionPlanById(Long planId);
    
    List<ProductionPlanResponseDto> getAllProductionPlans();
    
    ProductionPlanResponseDto updateProductionPlanStatus(Long planId, String status);
    
    List<RawMaterialRequirementDto> calculateRawMaterialRequirements(Long planId);
    
    Double calculateTotalRawMaterialCost(Long planId);
    
    void deleteProductionPlan(Long planId);

    List<ProductionPlanResponseDto> getProductionPlanTemplates();

    ProductionPlanResponseDto updateProductionPlan(Long planId, ProductionPlanRequestDto requestDto);

    EnhancedProductionPlanResponseDto previewProductionPlanMaterials(ProductionPlanRequestDto requestDto);
}
