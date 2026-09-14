package com.plover.backerymanagmentsystem.worker.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MpcMaterialRequestResponseDto {
    private Long id;
    private String requestCode;
    private String mpcName;
    private String outletName;
    private String status; // PENDING_MANAGER, APPROVED_MANAGER, ISSUED, RECEIVED, CANCELLED
    private String createdAt;
    private String notes;
    private List<ItemDto> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemDto {
        private Long rawMaterialId;
        private String rawMaterialName;
        private Double requestedQty;
        private Double issuedQty;
        private String unitOfMeasure;
    }
}
