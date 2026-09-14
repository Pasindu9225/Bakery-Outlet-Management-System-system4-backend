package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for processing an item return or exchange
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDto {

    private Integer saleId;
    private String returnReason;
    private String refundType; // "REFUND" or "EXCHANGE"
    private UUID cashierId;
    private Integer paymentMethodId;
    private BigDecimal totalReturnAmount;
    private BigDecimal totalExchangeAmount;
    private BigDecimal netRefundAmount;
    private List<ReturnItemDto> returnItems;
    private List<ExchangeItemDto> exchangeItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDto {
        private Integer saleItemId;
        private Integer qty;
        private BigDecimal unitPrice;
        private Boolean isResellable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExchangeItemDto {
        private Integer dayProductionItemId;
        private Integer qty;
        private BigDecimal unitPrice;
    }
}
