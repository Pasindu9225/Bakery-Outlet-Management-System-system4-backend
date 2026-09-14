// package com.plover.backerymanagmentsystem.store_keeper.service;
// import java.util.List;
// import com.plover.backerymanagmentsystem.store_keeper.dto.CreateStockAdjustmentRequestDto;
// import com.plover.backerymanagmentsystem.store_keeper.dto.StockAdjustmentResponseDto;
// import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;
// /**
//  * Service interface for managing stock adjustment operations. Handles the
//  * creation and management of stock adjustments with proper business logic
//  * validation and stock tracking.
//  */
// public interface StockAdjustmentService {
//     /**
//      * Creates a new stock adjustment record.
//      *
//      * Validates: - Raw material exists and is active - User exists in the
//      * system - Adjustment quantity is valid - Records the current stock before
//      * adjustment
//      *
//      * @param requestDto the stock adjustment creation request
//      * @return StockAdjustmentResponseDto with created adjustment details
//      * @throws RawMaterialNotFoundException if raw material doesn't exist
//      * @throws StockAdjustmentException if validation fails or user not found
//      */
//     StockAdjustmentResponseDto createStockAdjustment(CreateStockAdjustmentRequestDto requestDto);
//     /**
//      * Retrieves all stock adjustments for a specific raw material.
//      *
//      * @param rawMaterialId the raw material ID
//      * @return list of stock adjustments ordered by creation date (newest first)
//      * @throws RawMaterialNotFoundException if raw material doesn't exist
//      */
//     List<StockAdjustmentResponseDto> getStockAdjustmentsByRawMaterial(Long rawMaterialId);
//     /**
//      * Retrieves all stock adjustments with a specific status.
//      *
//      * @param status the adjustment status to filter by
//      * @return list of stock adjustments with the given status
//      */
//     List<StockAdjustmentResponseDto> getStockAdjustmentsByStatus(StockAdjustmentStatus status);
//     /**
//      * Retrieves all pending stock adjustments.
//      *
//      * @return list of pending stock adjustments
//      */
//     List<StockAdjustmentResponseDto> getAllPendingAdjustments();
//     /**
//      * Retrieves a specific stock adjustment by ID.
//      *
//      * @param adjustmentId the adjustment ID
//      * @return stock adjustment details
//      * @throws StockAdjustmentNotFoundException if adjustment doesn't exist
//      */
//     StockAdjustmentResponseDto getStockAdjustmentById(Long adjustmentId);
//     /**
//      * Retrieves all stock adjustments with relevant raw material names.
//      *
//      * @return list of all stock adjustments ordered by creation date (newest
//      * first)
//      */
//     List<StockAdjustmentResponseDto> getAllStockAdjustments();
// }
//new code
package com.plover.backerymanagmentsystem.store_keeper.service;

import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.dto.CreateStockAdjustmentRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.StockAdjustmentResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;

/**
 * Service interface for managing stock adjustment operations. Handles the
 * creation and management of stock adjustments with proper business logic
 * validation and stock tracking.
 */
public interface StockAdjustmentService {

    /**
     * Creates a new stock adjustment record.
     *
     * Validates: - Raw material exists and is active - User exists in the
     * system - Adjustment quantity is valid - Records the current stock before
     * adjustment
     *
     * @param requestDto the stock adjustment creation request
     * @return StockAdjustmentResponseDto with created adjustment details
     * @throws RawMaterialNotFoundException if raw material doesn't exist
     * @throws StockAdjustmentException if validation fails or user not found
     */
    StockAdjustmentResponseDto createStockAdjustment(CreateStockAdjustmentRequestDto requestDto);

    /**
     * Retrieves all stock adjustments for a specific raw material.
     *
     * @param rawMaterialId the raw material ID
     * @return list of stock adjustments ordered by creation date (newest first)
     * @throws RawMaterialNotFoundException if raw material doesn't exist
     */
    List<StockAdjustmentResponseDto> getStockAdjustmentsByRawMaterial(Long rawMaterialId);

    /**
     * Retrieves all stock adjustments with a specific status.
     *
     * @param status the adjustment status to filter by
     * @return list of stock adjustments with the given status
     */
    List<StockAdjustmentResponseDto> getStockAdjustmentsByStatus(StockAdjustmentStatus status);

    /**
     * Retrieves all pending stock adjustments.
     *
     * @return list of pending stock adjustments
     */
    List<StockAdjustmentResponseDto> getAllPendingAdjustments();

    /**
     * Retrieves a specific stock adjustment by ID.
     *
     * @param adjustmentId the adjustment ID
     * @return stock adjustment details
     * @throws StockAdjustmentNotFoundException if adjustment doesn't exist
     */
    StockAdjustmentResponseDto getStockAdjustmentById(Long adjustmentId);

    /**
     * Retrieves all stock adjustments with relevant raw material names.
     *
     * @return list of all stock adjustments ordered by creation date (newest
     * first)
     */
    List<StockAdjustmentResponseDto> getAllStockAdjustments();
}
