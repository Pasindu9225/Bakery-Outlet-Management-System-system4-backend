package com.plover.backerymanagmentsystem.store_keeper.service;

import org.springframework.http.ResponseEntity;

import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsWithMiniStoreRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueMaterialsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionPlanMaterialSummaryResponse;
import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialRequirementsResponse;
import com.plover.backerymanagmentsystem.store_keeper.dto.EnhancedProductionPlanSummaryResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionPlanSummaryResponseDto;

import java.util.List;
import com.plover.backerymanagmentsystem.store_keeper.dto.ProductionCenterResponseDto;

public interface StoreKeeperProductionPlanService {

    /**
     * Retrieves all production centers in the system.
     * Returns a list of production center IDs and names.
     *
     * @return List of ProductionCenterResponseDto
     */
    List<ProductionCenterResponseDto> getAllProductionCenters();

    /**
     * Gets raw material requirements for all approved production plans.
     * Aggregates quantities and costs for materials, split by kitchen and
     * bakery.
     *
     * @return RawMaterialRequirementsResponse containing total quantities,
     * costs, and splits by production center
     */
    RawMaterialRequirementsResponse getApprovedProductionPlanMaterials();

    /**
     * Gets detailed material requirements organized by production plan. For
     * each approved production plan, lists all required materials with their
     * quantities split between kitchen and bakery.
     *
     * @return ProductionPlanMaterialSummaryResponse containing per-plan
     * material details
     */
    ProductionPlanMaterialSummaryResponse getProductionPlanWiseMaterialSummary();

    /**
     * Issues materials for a specific approved production plan.
     *
     * @param requestDto containing the production plan ID
     * @return IssueMaterialsResponseDto with success details and updated stock
     * information
     */
    IssueMaterialsResponseDto issueMaterialsForApprovedPlans(IssueMaterialsWithMiniStoreRequestDto requestDto);

    /**
     * Gets enhanced production plan details with BOM trees for all approved production plans.
     * Returns detailed BOM breakdown with scaled quantities and costs.
     *
     * @return EnhancedProductionPlanSummaryResponseDto containing production plans with BOM trees
     */
    EnhancedProductionPlanSummaryResponseDto getEnhancedProductionPlanSummary();

    /**
     * Gets production plan summary with consolidated semi-products and hierarchical structure.
     * Returns detailed breakdown with semi-product consolidation and usage breakdown.
     *
     * @return ProductionPlanSummaryResponseDto containing consolidated production plan data
     */
    ProductionPlanSummaryResponseDto getProductionPlanSummary();

    com.plover.backerymanagmentsystem.store_keeper.dto.StorekeeperDashboardStatsResponseDto getDashboardStats();
}
