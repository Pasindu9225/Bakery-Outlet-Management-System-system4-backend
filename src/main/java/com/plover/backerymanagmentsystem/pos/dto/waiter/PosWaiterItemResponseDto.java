package com.plover.backerymanagmentsystem.pos.dto.waiter;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PosWaiterItemResponseDto {
    private Long id;
    private String waiterId;
    private Integer productId;
    private String productName;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String instructions;
    private Boolean isPaid;
    private Long kotId;
    private String billId;
}
