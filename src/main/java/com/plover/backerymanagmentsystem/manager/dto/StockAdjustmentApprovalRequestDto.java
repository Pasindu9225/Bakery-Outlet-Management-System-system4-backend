package com.plover.backerymanagmentsystem.manager.dto;

import java.util.UUID;

import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for approving or rejecting stock adjustments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentApprovalRequestDto {

    @NotNull(message = "Stock adjustment ID is required")
    private Long stockAdjustmentId;

    @NotNull(message = "Approval status is required")
    private StockAdjustmentStatus status;

    @NotNull(message = "Approver ID is required")
    private UUID approverId;

    private String remarks;
}
