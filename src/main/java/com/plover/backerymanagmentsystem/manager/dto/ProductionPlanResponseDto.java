package com.plover.backerymanagmentsystem.manager.dto;

import com.plover.backerymanagmentsystem.manager.model.ProductionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanResponseDto {
    
    private Long id;
    private String planName;
    private String department;
    private LocalDateTime planDate;
    private String status;
    private Double totalEstimatedCost;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProductionPlanItemResponseDto> productionItems;
    private List<RawMaterialRequirementDto> rawMaterialRequirements;
    private Double totalRawMaterialCost;
    private Boolean isTemplate;
    private List<DistributionPlanDto> distributionPlans;
    
    public static ProductionPlanResponseDto fromEntity(ProductionPlan productionPlan) {
        return ProductionPlanResponseDto.builder()
                .id(productionPlan.getId())
                .planName(productionPlan.getPlanName())
                .department(productionPlan.getDepartment())
                .planDate(productionPlan.getPlanDate())
                .status(productionPlan.getStatus() != null ? productionPlan.getStatus().name() : null)
                .totalEstimatedCost(productionPlan.getTotalEstimatedCost())
                .notes(productionPlan.getNotes())
                .createdAt(productionPlan.getCreatedAt())
                .updatedAt(productionPlan.getUpdatedAt())
                .build();
    }
}
