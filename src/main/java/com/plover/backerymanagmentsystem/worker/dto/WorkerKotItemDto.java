package com.plover.backerymanagmentsystem.worker.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkerKotItemDto {
    private Long id;
    private String productName;
    private Long productId;
    private Integer plannedQuantity;
    private Integer completedQuantity;
    private Long productionCenterId;
}
