package com.plover.backerymanagmentsystem.manager.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for stock adjustment approval operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentApprovalResponseDto {

    private Long stockAdjustmentId;
    private Long rawMaterialId;
    private String rawMaterialName;
    private Double changeQuantity;
    private Double beforeQuantity;
    private Double afterQuantity;
    private Double updatedStockLevel;
    private StockAdjustmentStatus status;
    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String message;
}