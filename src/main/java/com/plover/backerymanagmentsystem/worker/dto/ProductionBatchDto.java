package com.plover.backerymanagmentsystem.worker.dto;

import java.time.LocalDateTime;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductionBatchDto {
    private Long id;
    private Long productionPlanItemId;
    private String productName;
    private Integer producedQty;
    private Integer wastageQty;
    private String wastageReason;
    private Integer plannedQty;
    private Integer remainingQty;
    private String notes;
    private LocalDateTime createdAt;
}
