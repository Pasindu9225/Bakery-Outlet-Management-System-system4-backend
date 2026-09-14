package com.plover.backerymanagmentsystem.admin.dto;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductionCenterResponse {
    private Long id;
    private String centerName;
    private String location;
    private Boolean isActive;
    private String miniStoreName;
    private ProductionCenterType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private java.time.LocalDate establishedDate;
}
