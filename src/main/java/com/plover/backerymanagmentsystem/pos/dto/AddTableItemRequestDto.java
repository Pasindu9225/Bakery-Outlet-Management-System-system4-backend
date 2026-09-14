package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTableItemRequestDto {
    private Long tableId;
    private Integer productId;
    private String productName;
    private Integer qty;
    private BigDecimal unitPrice;
    private String instructions;
    private Boolean isPaid;
}
