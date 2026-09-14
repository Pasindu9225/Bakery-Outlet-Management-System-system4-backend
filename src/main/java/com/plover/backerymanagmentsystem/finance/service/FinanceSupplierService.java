package com.plover.backerymanagmentsystem.finance.service;

import java.util.List;

import com.plover.backerymanagmentsystem.finance.dto.SupplierForLedgerDto;

/**
 * Service exposing supplier lookups tailored for the finance module.
 */
public interface FinanceSupplierService {

    /**
     * Return all suppliers in the system as lightweight projections suitable
     * for populating the ledger supplier picker.
     *
     * @return all suppliers
     */
    List<SupplierForLedgerDto> getAllSuppliersForLedger();
}
