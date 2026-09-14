package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinishTableBillingRequestDto {
    private Long tableId;
    private UUID cashierId;
    private List<SaleItemRequestDto> items;
    private BigDecimal amountReceived;
    private Integer paymentMethodId;
    private BigDecimal totalAmount;
    private Long globalPromotionId;
}
