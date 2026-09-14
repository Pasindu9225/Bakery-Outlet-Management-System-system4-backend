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
 * Response DTO for sale creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSaleResponseDto {

    private boolean success;
    private String message;
    private SaleDataDto data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleDataDto {

        private Integer saleId;
        private String billNumber;
        private LocalDate saleDate;
        private LocalTime saleTime;
        private UUID cashierId;
        private String cashierName;
        private BigDecimal totalAmount;
        private BigDecimal receivedAmount;
        private BigDecimal changeAmount;
        private List<SaleItemResponseDto> items;
        private List<String> kotNumbers;
    }
}
