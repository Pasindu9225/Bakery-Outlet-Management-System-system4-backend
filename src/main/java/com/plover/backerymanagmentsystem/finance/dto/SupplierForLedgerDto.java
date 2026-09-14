package com.plover.backerymanagmentsystem.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight supplier projection for the ledger supplier picker.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierForLedgerDto {

    /** Display id, e.g. {@code "SUP-001"}. */
    private String id;

    /** Raw supplier id used by ledger queries. */
    private Long supplierId;

    private String name;

    /** Up to 3 uppercase letters derived from the supplier name. */
    private String code;
}
