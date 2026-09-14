package com.plover.backerymanagmentsystem.admin.dto;

import java.time.LocalDate;

import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductionCenterRequest {
    private String productionCenterName;
    private String location;
    private Integer miniStoreId;
    private String miniStoreName;
    private LocalDate establishedDate;
    private Boolean isActive;
    private ProductionCenterType type;
}
