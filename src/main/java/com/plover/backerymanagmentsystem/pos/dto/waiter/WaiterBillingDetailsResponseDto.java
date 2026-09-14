package com.plover.backerymanagmentsystem.pos.dto.waiter;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WaiterBillingDetailsResponseDto {
    private String waiterId;
    private String waiterName;
    private List<PosWaiterItemResponseDto> unpaidItems;
    private BigDecimal subTotal;
}
