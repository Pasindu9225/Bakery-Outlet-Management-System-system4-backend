package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for detailed purchase order response with all items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedPurchaseOrderResponseDto {

    private List<PurchaseOrderWithItemsDto> purchaseOrders;
    private Integer totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderWithItemsDto {
        private Long poId;
        private LocalDate estimatedDeliveryDate;
        private Integer numberOfItems;
        private Long supplierId;
        private BigDecimal totalCost;
        private String status;
        private String supplierName;
        private List<PurchaseOrderItemDetailDto> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderItemDetailDto {
        private Long poiId;
        private BigDecimal actualCost;
        private BigDecimal estimatedCost;
        private Long rawMaterialId;
        private Integer receivedQty;
        private Integer requiredQty;
        private String unitOfMeasure;
        private String rawMaterialName;
    }
}
