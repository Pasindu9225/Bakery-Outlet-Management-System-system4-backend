package com.plover.backerymanagmentsystem.store_keeper.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueMaterialsRequestDto {

    
    @NotNull(message = "Production plan ID is required")
    @Positive(message = "Production plan ID must be positive")
    private Long productionPlanId;
}
