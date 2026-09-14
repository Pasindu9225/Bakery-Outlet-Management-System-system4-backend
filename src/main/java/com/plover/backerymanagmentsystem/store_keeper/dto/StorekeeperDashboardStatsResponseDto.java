package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorekeeperDashboardStatsResponseDto {
    private List<ProductionPlanSummaryResponseDto.ProductionPlanDto> pendingManagerRequests;
    private List<RecentTransactionDto> recentTransactions;
    private List<LowStockMaterialsResponseDto.LowStockMaterialDto> stockAlerts;
    private Long pendingWorkerRequestsCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTransactionDto {
        private String id;
        private String type;
        private String supplier;
        private String date;
        private Integer items;
        private String amount;
        private String status;
        private LocalDateTime createdAt;
    }
}
