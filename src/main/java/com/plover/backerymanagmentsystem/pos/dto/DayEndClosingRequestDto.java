package com.plover.backerymanagmentsystem.pos.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayEndClosingRequestDto {
    private Long outletId;
    private UUID cashierId;
    private List<ClosingItemRequestDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClosingItemRequestDto {
        private Long productId;
        private Integer systemQty;
        private Integer physicalQty;
        private Integer carryForwardQty;
        private Integer wastageQty;
    }
}
