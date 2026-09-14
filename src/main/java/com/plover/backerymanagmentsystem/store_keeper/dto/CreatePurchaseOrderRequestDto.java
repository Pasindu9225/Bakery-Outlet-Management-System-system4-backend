package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a purchase order request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseOrderRequestDto {

    @NotNull(message = "Supplier ID is required")
    @Positive(message = "Supplier ID must be positive")
    private Long supplierId;

    @NotNull(message = "Estimated delivery date is required")
    @Future(message = "Estimated delivery date must be in the future")
    private LocalDate estimatedDeliveryDate;

    @NotEmpty(message = "Purchase order must contain at least one item")
    @Valid
    private List<PurchaseOrderItemRequestDto> items;

    private String status;

    /**
     * Nested DTO for purchase order item request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderItemRequestDto {

        @NotNull(message = "Raw material ID is required")
        @Positive(message = "Raw material ID must be positive")
        private Long rawMaterialId;

        @NotNull(message = "Required quantity is required")
        @Positive(message = "Required quantity must be positive")
        private Double requiredQty;

        @NotNull(message = "Unit of measure is required")
        private String unitOfMeasure;

        @NotNull(message = "Actual cost is required")
        @Positive(message = "Actual cost must be positive")
        private BigDecimal actualCost;
    }
}
