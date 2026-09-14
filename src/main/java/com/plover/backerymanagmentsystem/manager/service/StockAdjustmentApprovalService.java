package com.plover.backerymanagmentsystem.manager.service;

import com.plover.backerymanagmentsystem.manager.dto.StockAdjustmentApprovalRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.StockAdjustmentApprovalResponseDto;

/**
 * Service interface for managing stock adjustment approvals. Handles the
 * approval and rejection of stock adjustments with proper business logic
 * validation and stock updates.
 */
public interface StockAdjustmentApprovalService {

    /**
     * Approves or rejects a stock adjustment.
     *
     * For APPROVED status: - Updates the raw material stock by applying the
     * change quantity - Sets the approval information (approver, timestamp) -
     * Updates the stock adjustment status
     *
     * For REJECTED status: - Only updates the stock adjustment status and
     * approval information - Does not modify raw material stock
     *
     * @param requestDto the approval request containing adjustment ID, status,
     * and approver
     * @return StockAdjustmentApprovalResponseDto with updated information
     * @throws StockAdjustmentNotFoundException if adjustment doesn't exist
     * @throws StockAdjustmentException if validation fails or invalid status
     * transition
     */
    StockAdjustmentApprovalResponseDto processStockAdjustmentApproval(StockAdjustmentApprovalRequestDto requestDto);
}
