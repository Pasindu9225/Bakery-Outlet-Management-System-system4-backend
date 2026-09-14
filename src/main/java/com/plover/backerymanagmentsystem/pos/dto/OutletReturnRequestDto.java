package com.plover.backerymanagmentsystem.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletReturnRequestDto {
    private Long outletId;
    private String reason;
    private String remarks;
    private UUID initiatorId;
    private List<ReturnItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDto {
        private Long productId;
        private Integer dayProductionItemId;
        private Integer qty;
        private String batchNote;
    }
}
