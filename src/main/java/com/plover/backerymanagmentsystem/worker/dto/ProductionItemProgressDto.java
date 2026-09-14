package com.plover.backerymanagmentsystem.worker.dto;

import java.util.List;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductionItemProgressDto {
    private Long productionPlanItemId;
    private Long productionPlanId;
    private String productName;
    private Integer plannedQty;
    private Integer producedQty;     // sum of batches
    private Integer wastageQty;       // sum of batches
    private Integer remainingQty;     // planned - produced
    private List<ProductionBatchDto> batches;
}
