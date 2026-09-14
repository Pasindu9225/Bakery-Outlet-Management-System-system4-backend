package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to record a supplier payment with one or more allocations
 * against open GRNs. The {@link #paymentMethod} accepts both display values
 * (e.g. {@code "Bank Transfer"}) and canonical enum values (e.g.
 * {@code "BANK_TRANSFER"}); normalisation happens in the service layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequestDto {

    @NotNull(message = "supplierId is required")
    private Long supplierId;

    @NotNull(message = "paymentDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate paymentDate;

    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;

    private String remarks;

    @Valid
    @NotEmpty(message = "at least one allocation is required")
    private List<AllocationDto> allocations;

    /**
     * One allocation line of a payment. Targets a single GRN.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationDto {

        @NotNull(message = "grnId is required")
        private Long grnId;

        @NotNull(message = "allocatedAmount is required")
        @Positive(message = "allocatedAmount must be positive")
        private BigDecimal allocatedAmount;
    }
}
