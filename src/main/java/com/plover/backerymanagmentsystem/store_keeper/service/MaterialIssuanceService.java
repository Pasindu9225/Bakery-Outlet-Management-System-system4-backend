package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsResponseDto;

/**
 * Service interface for material issuance operations. Follows Single Responsib
 * lity Principle by handling only material issuance logic.
 */
public interface MaterialIssuanceService {

    
    /**
     *
     * Issues materials for an approved production plan.
     *
     * @param requestDto containing the production plan ID
     * @return IssueMaterialsResponseDto with success details and updated stock
     * information  @throws ProductionPlanNotFoundException if production plan doesn't exist
     * @throws ProductionPlanNotApprovedException if production plan is not
     * approved
     * @throws InsufficientStockException if any material has @throws MaterialsAlreadyIssuedException if materials have already been
     * issued
     */
    IssueMaterialsResponseDto issueMaterialsForProductionPlan(com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto requestDto);
}
