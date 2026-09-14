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
public class PosDashboardStatsResponseDto {
    private BigDecimal totalSalesToday;
    private Integer totalOrdersToday;
    private BigDecimal cashSalesToday;
    private BigDecimal cardSalesToday;
    private BigDecimal returnsToday;
    private List<GetAllSalesResponseDto.SaleSummaryDto> recentTransactions;
    private List<LowStockItemDto> lowStockItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockItemDto {
        private String name;
        private Integer current;
        private Integer minimum;
        private String status; // "critical", "warning"
    }
}
