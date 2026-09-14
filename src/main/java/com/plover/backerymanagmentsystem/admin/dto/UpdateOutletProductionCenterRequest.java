package com.plover.backerymanagmentsystem.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOutletProductionCenterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private Boolean isActive;
}
