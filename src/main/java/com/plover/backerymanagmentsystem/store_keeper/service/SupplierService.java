package com.plover.backerymanagmentsystem.store_keeper.service;

import com.plover.backerymanagmentsystem.store_keeper.dto.AllSuppliersResponseDto;

/**
 * Service interface for managing supplier operations in the store keeper
 * module. Provides operations for retrieving supplier information.
 */
public interface SupplierService {

    /**
     * Retrieves all suppliers from the system with their complete information.
     *
     * @return AllSuppliersResponseDto containing list of all suppliers and
     * total count
     * @throws SuppliersNotFoundException if no suppliers are found in the
     * system
     */
    AllSuppliersResponseDto getAllSuppliers();
}
