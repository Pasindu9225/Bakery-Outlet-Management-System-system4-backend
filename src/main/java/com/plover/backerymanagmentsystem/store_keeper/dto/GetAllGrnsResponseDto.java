package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for retrieving all GRNs with their basic information and item
 * counts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAllGrnsResponseDto {

    private List<GrnSummaryDto> grns;
    private Integer totalCount;

    /**
     * Summary information for a single GRN including item count.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrnSummaryDto {

        private Long grnId;
        private LocalDateTime receivedDate;
        private Long poId;
        private Long supplierId;
        private String supplierName;
        private BigDecimal total;
        private GrnStatus grnStatus;
        private Boolean isReceived;
        private String invoiceNumber;
        private Integer numberOfItems;

        // Additional useful information
        private LocalDateTime createdAt;
        private String poReference;
        private String materialNames;
        private java.time.LocalDate estimatedDeliveryDate;
        private String storekeeperSignature;
    }
}
