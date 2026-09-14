package com.plover.backerymanagmentsystem.worker.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RecordBatchDto {
    @NotNull private Long productionPlanItemId;
    @NotNull @Min(1) private Integer producedQty;
    @Min(0) @Builder.Default
    private Integer wastageQty = 0;
    private String wastageReason;
    private String notes;
}
