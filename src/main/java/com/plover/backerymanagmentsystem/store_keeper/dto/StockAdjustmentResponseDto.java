package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentReason;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for stock adjustment operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentResponseDto {

    private Long id;
    private Long rawMaterialId;
    private String rawMaterialName;
    private Double changeQuantity;
    private Double beforeQuantity;
    private Double afterQuantity;
    private StockAdjustmentReason reasonForAdjust;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private StockAdjustmentStatus status;
    private UUID approvedBy;
    private String approvedByName;
    private UUID addedBy;
    private String addedByName;
}
