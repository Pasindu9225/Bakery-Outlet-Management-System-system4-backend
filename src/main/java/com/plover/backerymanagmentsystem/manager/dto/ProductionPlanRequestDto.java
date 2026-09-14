package com.plover.backerymanagmentsystem.manager.dto;

import java.time.OffsetDateTime;
import java.util.List;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanRequestDto {

    @NotBlank(message = "Plan name is required")
    private String planName;

    private String department;

    @NotNull(message = "Plan date is required")
    private OffsetDateTime planDate;

    private String notes;

    @NotEmpty(message = "At least one production item is required")
    private List<ProductionPlanItemRequestDto> productionItems;

    // Expected values: DRAFT or SUBMITTED. Defaults to DRAFT if null/invalid.
    private String status;

    private Boolean isTemplate;
}
