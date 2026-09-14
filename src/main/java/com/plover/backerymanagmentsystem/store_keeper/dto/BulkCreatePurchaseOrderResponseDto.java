package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for bulk purchase order creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreatePurchaseOrderResponseDto {

    private List<CreatePurchaseOrderResponseDto> createdPurchaseOrders;
    private int totalOrdersCreated;
    private LocalDateTime processedAt;
}
