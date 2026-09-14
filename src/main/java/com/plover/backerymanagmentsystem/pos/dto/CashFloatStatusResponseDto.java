package com.plover.backerymanagmentsystem.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CashFloatStatusResponseDto {
    private boolean isOpened;
    private boolean isClosed;
    private BigDecimal openingBalance;
    private LocalDateTime openingTimestamp;
}
