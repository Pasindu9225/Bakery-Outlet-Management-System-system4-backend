package com.plover.backerymanagmentsystem.pos.dto.waiter;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AddWaiterItemRequestDto {
    private String waiterId;
    private Integer productId;
    private Integer qty;
    private BigDecimal unitPrice;
    private String instructions;
    private Long productionCenterId;
    private String billId;
}
