package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.RawMaterialSuppliersResponseDto;

/**
 * Service interface for managing raw material suppliers operations. Follows
 * Single Responsibility Principle by handling only supplier-related queries.
 */
public interface RawMaterialSuppliersService {

    /**
     * Retrieves all suppliers for a specific raw material with their negotiated
     * terms.
     *
     * @param rawMaterialId the ID of the raw material to get suppliers for
     * @return RawMaterialSuppliersResponseDto containing supplier information
     * @throws RawMaterialNotFoundException if raw material doesn't exist
     * @throws NoSuppliersFoundException if no suppliers found for the material
     */
    RawMaterialSuppliersResponseDto getSuppliersByRawMaterial(Long rawMaterialId);
}
