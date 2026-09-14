package com.plover.backerymanagmentsystem.worker.dto;

import java.util.List;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateKitchenReturnDto {
    private String notes;
    @NotEmpty private List<Item> items;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        @NotNull private Long rawMaterialId;
        @NotNull @DecimalMin("0.01") private Double quantity;
        private String reason;
    }
}
