package com.plover.backerymanagmentsystem.worker.dto;

import java.util.List;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTransferNoteDto {
    private Long destinationOutletId;
    private Long destinationMpcId;
    private String notes;
    @NotEmpty private List<Item> items;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        @NotNull private Long productId;
        private String productName;
        @NotNull @Min(1) private Integer quantity;
        private String unit;
    }
}
