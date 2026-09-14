package com.plover.backerymanagmentsystem.pos.dto;

import com.plover.backerymanagmentsystem.pos.model.OutletReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletReturnDetailDto {
    private Long id;
    private String returnNoteId;
    private Long outletId;
    private String outletName;
    private OutletReturnStatus status;
    private String reason;
    private String remarks;
    private UUID initiatorId;
    private String initiatorName;
    private LocalDateTime createdAt;
    private List<ReturnItemDetailDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDetailDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productCode;
        private Integer qty;
        private String batchNote;
    }
}
