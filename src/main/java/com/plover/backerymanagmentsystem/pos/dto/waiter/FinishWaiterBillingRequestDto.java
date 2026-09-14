package com.plover.backerymanagmentsystem.pos.dto.waiter;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class FinishWaiterBillingRequestDto {
    private String waiterId;
    private List<Long> itemIdsToPay;
    private BigDecimal finalTotal;
    private String paymentType;
    private BigDecimal discountAmount;
    private String discountReason;
    private Long outletId;
    private Boolean invoicePrinted;
}
