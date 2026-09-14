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
 * Response DTO for retrieving a single sale by ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSaleByIdResponseDto {

    private Integer saleId;
    private String billNumber;
    private LocalDate saleDate;
    private LocalTime saleTime;
    private UUID cashierId;
    private String cashierName;
    private BigDecimal totalAmount;
    private Boolean invoicePrinted;
    private List<SaleItemDetailDto> saleItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemDetailDto {

        private Integer saleItemId;
        private Integer dayProductionItemId;
        private String productName;
        private String productCode;
        private Integer qty;
        private BigDecimal price;
        private String freeMealReason;
        private String bankTransferCode;
        private Integer discountId;
        private String discountName;
        private BigDecimal discountPercentage;
        private Integer paymentMethodId;
        private String paymentMethodName;
        private UUID cashierId;
        private String cashierName;
    }
}
