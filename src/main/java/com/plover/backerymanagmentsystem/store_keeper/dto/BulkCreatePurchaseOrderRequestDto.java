package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bulk purchase order creation across multiple suppliers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreatePurchaseOrderRequestDto {

    @NotEmpty(message = "At least one purchase order request is required")
    @Valid
    private List<CreatePurchaseOrderRequestDto> purchaseOrders;
}
