package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionPlanItemDto {
    
    private Long dpiId;
    private Long productId;
    private String productName;
    private Integer qty;
    private Long dpId;
}
