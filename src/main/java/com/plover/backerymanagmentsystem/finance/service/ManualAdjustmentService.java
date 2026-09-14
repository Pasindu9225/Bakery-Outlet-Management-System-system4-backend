package com.plover.backerymanagmentsystem.finance.service;

import com.plover.backerymanagmentsystem.finance.dto.CreateManualAdjustmentRequestDto;
import com.plover.backerymanagmentsystem.finance.dto.ManualAdjustmentResponseDto;

/**
 * Service for creating manual supplier-ledger adjustments. Restricted to the
 * Finance role at the controller layer.
 */
public interface ManualAdjustmentService {

    /**
     * Persist a manual ledger adjustment. Validates that remarks are provided.
     *
     * @param request the adjustment request
     * @return the persisted adjustment as a response DTO
     */
    ManualAdjustmentResponseDto createAdjustment(CreateManualAdjustmentRequestDto request);
}
