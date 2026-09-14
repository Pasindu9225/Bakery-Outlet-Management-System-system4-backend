package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllPurchaseOrdersResponseDto {

    private List<PurchaseOrderDetailDto> purchaseOrders;
    private Integer totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderDetailDto {
        private Long poId;
        private BigDecimal totalCost;
        private Integer numberOfItems;
        private LocalDate estimatedDeliveryDate;
        private Long supplierId;
        private String supplierName;
        // Status from purchase order table
        private String poStatus;
        private List<PurchaseOrderItemDto> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderItemDto {
        private Long poiId;
        private Long rawMaterialId;
        private String rawMaterialName;
        private Integer requiredQty;
        private Integer receivedQty;
        private BigDecimal actualCost;
        private BigDecimal estimatedCost;
        private String unitOfMeasure;
    }
}


