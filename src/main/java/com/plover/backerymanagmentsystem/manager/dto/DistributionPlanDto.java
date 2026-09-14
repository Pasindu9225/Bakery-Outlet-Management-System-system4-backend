package com.plover.backerymanagmentsystem.manager.dto;

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
public class DistributionPlanDto {
    
    private Long dpId;
    private String name;
    private Long outletId;
    private String outletName;
    private LocalDate date;
    private Boolean isActive;
    private String status;
    private List<DistributionPlanItemDto> items;
}
