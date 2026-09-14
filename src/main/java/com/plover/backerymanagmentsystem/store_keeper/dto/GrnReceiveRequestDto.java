package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for GRN receive request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrnReceiveRequestDto {

    @NotNull(message = "GRN status is required")
    private GrnStatus grnStatus;

    private String invoiceNumber;

    private String storekeeperSignature;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<GrnReceiveItemDto> items;

    /**
     * Nested DTO for individual GRN item receive request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrnReceiveItemDto {

        @NotNull(message = "GRN item ID is required")
        private Long grnItemId;

        @NotNull(message = "Received quantity is required")
        @PositiveOrZero(message = "Received quantity must be zero or positive")
        private BigDecimal receivedQuantity;

        private BigDecimal invoiceQuantity;
        private BigDecimal actualQuantity;

        private BigDecimal invoicePrice;
        private BigDecimal actualPrice;

        private String batchNo;

        private java.time.LocalDate expireDate;
    }
}
