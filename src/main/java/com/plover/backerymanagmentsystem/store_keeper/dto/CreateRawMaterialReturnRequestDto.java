package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.time.LocalDate;
import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.model.ReturnReason;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a raw material return.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRawMaterialReturnRequestDto {

    @NotNull(message = "Return date is required")
    private LocalDate returnDate;

    @NotNull(message = "Supplier ID is required")
    @Positive(message = "Supplier ID must be positive")
    private Long supplierId;

    @NotEmpty(message = "Return items list cannot be empty")
    @Valid
    private List<ReturnItemRequestDto> returnItems;

    /**
     * Nested DTO for individual return item request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequestDto {

        @NotNull(message = "Raw material ID is required")
        @Positive(message = "Raw material ID must be positive")
        private Long rawMaterialId;

        @NotNull(message = "Return quantity is required")
        @Positive(message = "Return quantity must be positive")
        private Double returnQuantity;

        @NotNull(message = "Return reason is required")
        private ReturnReason reason;
    }
}
