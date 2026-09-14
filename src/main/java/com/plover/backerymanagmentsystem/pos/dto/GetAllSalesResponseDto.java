package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for retrieving all sales
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAllSalesResponseDto {

    private List<SaleSummaryDto> sales;
    private Integer totalCount;
    private BigDecimal totalSalesAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleSummaryDto {

        private Integer saleId;
        private LocalDate saleDate;
        private LocalTime saleTime;
        private UUID cashierId;
        private String cashierName;
        private BigDecimal totalAmount;
        private Integer itemCount;
        private Boolean invoicePrinted;
    }
}
