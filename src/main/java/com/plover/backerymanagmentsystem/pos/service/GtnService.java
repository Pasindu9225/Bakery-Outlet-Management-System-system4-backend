package com.plover.backerymanagmentsystem.pos.service;

import com.plover.backerymanagmentsystem.pos.dto.CreateGtnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateGtnResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetGtnProductsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetPartiallyReceivedItemsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GtnReceiveRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.GtnReceiveResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ManualEntryRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.ManualEntryResponseDto;

/**
 * Service interface for GTN (Goods Transfer Note) operations
 */
public interface GtnService {

    /**
     * Get all GTN products that are not in 'received' or 'over received'
     * status. This includes GTN information (date, status, source, addedBy) and
     * GTN item information (expected qty, unit, expiry date) with product
     * names.
     *
     * @param outletId Optional outlet ID to filter GTNs.
     * @return GetGtnProductsResponseDto containing the list of GTNs with
     *         product information
     */
    GetGtnProductsResponseDto getGtnProducts(Long outletId);

    /**
     * Receive GTN items and update their status, create day production records
     *
     * @param requestDto The GTN receive request containing GTN ID and received
     *                   items
     * @return GtnReceiveResponseDto containing the result of the operation
     */
    GtnReceiveResponseDto receiveGtnItems(GtnReceiveRequestDto requestDto);

    /**
     * Get all partially received GTN items with full details
     *
     * @return GetPartiallyReceivedItemsResponseDto containing the list of
     *         partially received items
     */
    GetPartiallyReceivedItemsResponseDto getPartiallyReceivedItems();

    /**
     * Create a manual entry for a product
     *
     * @param requestDto The manual entry request containing product details,
     *                   quantity, unit, remarks, source, and user ID
     * @return ManualEntryResponseDto containing the result of the operation and
     *         details of the added product
     */
    ManualEntryResponseDto createManualEntry(ManualEntryRequestDto requestDto);

    /**
     * Create a new GTN for testing purposes
     *
     * @param requestDto The GTN creation request
     * @return CreateGtnResponseDto containing the result of the operation
     */
    CreateGtnResponseDto createGtn(CreateGtnRequestDto requestDto);
}
