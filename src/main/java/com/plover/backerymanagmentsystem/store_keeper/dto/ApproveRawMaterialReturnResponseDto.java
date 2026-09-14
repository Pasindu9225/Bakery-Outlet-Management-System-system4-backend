package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for raw material return approval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRawMaterialReturnResponseDto {

    private Integer approvedItemsCount;
    private Integer totalItemsCount;
    private BigDecimal totalApprovedValue;
    private List<ApprovedItemDto> approvedItems;
    private List<StockUpdateDto> stockUpdates;
    private LocalDateTime processedAt;

    /**
     * DTO for approved return item details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovedItemDto {

        private Long returnItemId;
        private Long rawMaterialId;
        private String rawMaterialName;
        private Double returnQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    /**
     * DTO for stock update information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockUpdateDto {

        private Long rawMaterialId;
        private String rawMaterialName;
        private Double previousStock;
        private Double returnedQuantity;
        private Double newStock;
    }
}
