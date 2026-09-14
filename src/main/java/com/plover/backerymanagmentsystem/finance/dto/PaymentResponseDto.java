package com.plover.backerymanagmentsystem.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for a single supplier payment, including its allocation
 * lines. {@code paymentMethod} is the human-friendly display form (e.g.
 * {@code "Bank Transfer"}) regardless of how it was stored.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Long paymentId;

    private String paymentRef;

    private LocalDate paymentDate;

    private Long supplierId;

    /** Joined supplier name (may be {@code null} if the supplier was deleted). */
    private String supplierName;

    /** Total payment amount (also equals sum of {@link #allocations} amounts). */
    private BigDecimal amount;

    /** Display form of the payment method (e.g. {@code "Bank Transfer"}). */
    private String paymentMethod;

    /** {@code Pending} or {@code Cleared}. */
    private String status;

    private String remarks;

    private LocalDateTime createdAt;

    private List<AllocationResponseDto> allocations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationResponseDto {

        private Long allocationId;

        private Long grnId;

        /** Display reference for the allocated GRN, e.g. {@code "GRN-12"}. */
        private String grnRef;

        private BigDecimal allocatedAmount;
    }
}
