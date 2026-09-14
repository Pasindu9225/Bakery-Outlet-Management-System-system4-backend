package com.plover.backerymanagmentsystem.manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionPlanRequestDto {
    
    @NotBlank(message = "Distribution plan name is required")
    private String name;
    
    @NotNull(message = "Outlet ID is required")
    private Long outletId;
    
    @NotNull(message = "Date is required")
    private LocalDate date;
    
    @NotNull(message = "Active status is required")
    private Boolean isActive;
    
    private String status;

    private Long productionPlanId;
    
    @NotEmpty(message = "Distribution plan items are required")
    @Valid
    private List<DistributionPlanItemRequestDto> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DistributionPlanItemRequestDto {
        
        @NotNull(message = "Product ID is required")
        private Long productId;
        
        @NotNull(message = "Quantity is required")
        private Integer qty;
    }
}
