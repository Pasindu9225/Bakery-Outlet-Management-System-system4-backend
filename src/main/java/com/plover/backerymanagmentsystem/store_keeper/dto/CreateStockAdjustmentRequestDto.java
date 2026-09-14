package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentReason;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a stock adjustment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAdjustmentRequestDto {

    @NotNull(message = "Raw material ID is required")
    @Positive(message = "Raw material ID must be positive")
    private Long rawMaterialId;

    @NotNull(message = "Adjustment date is required")
    private LocalDateTime date;

    @NotNull(message = "Adjustment quantity is required")
    private Double adjustmentQty;

    @NotNull(message = "Reason for adjustment is required")
    private StockAdjustmentReason reasonForAdjustment;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    @NotNull(message = "User ID is required")
    private UUID userId;
}
