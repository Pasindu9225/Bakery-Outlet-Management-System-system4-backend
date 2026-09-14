package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableBillingDetailsResponseDto {
    private PosTableResponseDto tableInfo;
    private List<PosTableItemResponseDto> unpaidItems;
    private BigDecimal totalAmount;
}
