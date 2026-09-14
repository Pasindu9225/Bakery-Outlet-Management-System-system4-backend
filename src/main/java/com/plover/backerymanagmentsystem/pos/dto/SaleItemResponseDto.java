package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for sale item information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemResponseDto {

    private Integer saleItemId;
    private Integer dayProductionItemId;
    private String productName;
    private String productCode;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String freeMealReason;
    private String bankTransferCode;
    private Integer discountId;
    private String discountName;
    private Integer paymentMethodId;
    private String paymentMethodName;
    private BigDecimal manualDiscount;
    private String itemStatus;
    private String specialInstructions;
}
