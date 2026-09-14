package com.plover.backerymanagmentsystem.pos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StandaloneKotRequestDto {
    @NotNull
    private Integer dayProductionItemId;

    @NotNull
    @Min(1)
    private Integer qty;

    @NotNull
    private Long productionCenterId;

    private String specialInstructions;
}
