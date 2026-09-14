package com.plover.backerymanagmentsystem.mis.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single row on the "Top Suppliers by Spend" ranking widget. Sorted by
 * {@code totalSpend} descending; the frontend slices the first 5.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierSpendDto {

    /** Raw supplier id — exposed so click-through can drive the supplier filter. */
    private Long supplierId;

    /** Display name. */
    private String supplierName;

    /** Sum of {@code totalCost} across the supplier's POs in the window. */
    private BigDecimal totalSpend;
}
