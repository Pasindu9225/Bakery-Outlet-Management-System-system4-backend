package com.plover.backerymanagmentsystem.manager.dto;

import com.plover.backerymanagmentsystem.manager.model.ProductionPlanBatchAllocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResult {
    private Double totalRequiredQty;
    private Double reservedQty;
    private Double netNeededQty; // shortfall to produce
    private Boolean fullyFulfilled;
    @Builder.Default
    private List<ProductionPlanBatchAllocation> allocations = new ArrayList<>();
}
