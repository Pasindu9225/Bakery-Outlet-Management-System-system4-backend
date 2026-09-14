package com.plover.backerymanagmentsystem.worker.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProductionRequestItemDto {
    private Long id;
    private String productName;
    private Long productId;
    private Long rawMaterialId;
    private Integer quantity;
    private Double exactQuantity;
    private String unit;
    private String unitOfMeasure;
    private Integer producedQuantity;
    private Double exactProducedQuantity;
    private Long productionCenterId;
    private String productionCenterName;
    private Long parentPlanItemId;
    private Boolean isLocalCenter;
    private Boolean isCompleted;
    private Boolean canProduceLocally;
    private Boolean isBlocked;
    private Integer uncompletedChildCount;
    private Double reservedFromMiniStore;
    private Boolean miniStoreFulfilled;
    @Builder.Default
    private List<WorkerProductionRequestItemDto> children = java.util.Collections.emptyList();
}
