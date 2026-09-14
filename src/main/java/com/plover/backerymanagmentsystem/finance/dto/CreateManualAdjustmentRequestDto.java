package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to create a manual ledger adjustment. Positive amounts are
 * debits, negative amounts are credits. Remarks are mandatory.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateManualAdjustmentRequestDto {

    @NotNull(message = "supplierId is required")
    private Long supplierId;

    @NotNull(message = "date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "remarks are required for manual adjustments")
    private String remarks;
}
