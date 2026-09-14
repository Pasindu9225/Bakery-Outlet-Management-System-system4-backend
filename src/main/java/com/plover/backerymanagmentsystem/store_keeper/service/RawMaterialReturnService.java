package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.ApproveRawMaterialReturnRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ApproveRawMaterialReturnResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateRawMaterialReturnRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateRawMaterialReturnResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.UpdateRawMaterialReturnRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GetAllRawMaterialReturnsResponseDto;

/**
 * Service interface for managing raw material return operations. Handles the
 * creation and approval of raw material returns with proper business logic
 * validation and stock management.
 */
public interface RawMaterialReturnService {

    /**
     * Creates a new raw material return with validation and cost calculation.
     *
     * Validates: - Supplier exists - Supplier can supply all specified raw
     * materials - Return quantities do not exceed available stock - Calculates
     * total cost based on original purchase prices or current unit costs
     *
     * @param requestDto the return creation request containing supplier, date,
     * and items
     * @return CreateRawMaterialReturnResponseDto with created return details
     * @throws SupplierNotFoundException if supplier doesn't exist
     * @throws SupplierMaterialValidationException if supplier cannot supply
     * specified materials
     * @throws InsufficientStockForReturnException if return quantities exceed
     * available stock
     * @throws RawMaterialNotFoundException if any raw material doesn't exist
     */
    CreateRawMaterialReturnResponseDto createRawMaterialReturn(CreateRawMaterialReturnRequestDto requestDto);

    /**
     * Approves specified return items and updates raw material stock.
     *
     * Business Logic: - Only processes return items with status 'NOT_APPROVED'
     * - Updates return item status to 'APPROVED' - Reduces current stock in
     * raw_materials table for approved items - Provides detailed stock update
     * information
     *
     * @param requestDto the approval request containing return item IDs
     * @return ApproveRawMaterialReturnResponseDto with approval details and
     * stock updates
     * @throws ReturnItemNotFoundException if any return item doesn't exist
     * @throws ReturnItemAlreadyApprovedException if attempting to approve
     * already approved items
     */
    ApproveRawMaterialReturnResponseDto approveReturnItems(ApproveRawMaterialReturnRequestDto requestDto);

    /**
     * Retrieves a raw material return by ID with all details.
     *
     * @param returnId the return ID
     * @return CreateRawMaterialReturnResponseDto with return details
     * @throws RawMaterialReturnNotFoundException if return doesn't exist
     */
    CreateRawMaterialReturnResponseDto getRawMaterialReturnById(Long returnId);

    /**
     * Retrieves all raw material returns with item and supplier details.
     *
     * @return aggregated response containing list of returns
     */
    GetAllRawMaterialReturnsResponseDto getAllRawMaterialReturns();

    /**
     * Updates an existing raw material return and its items.
     */
    CreateRawMaterialReturnResponseDto updateRawMaterialReturn(UpdateRawMaterialReturnRequestDto requestDto);
}
