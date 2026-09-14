package com.plover.backerymanagmentsystem.worker.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateIngredientRequestDto {
    private Long productionPlanId;
    private String notes;
    @NotEmpty private List<RequestItemInput> items;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RequestItemInput {
        @NotNull private Long rawMaterialId;
        @NotNull private Double requestedQty;
    }
}
