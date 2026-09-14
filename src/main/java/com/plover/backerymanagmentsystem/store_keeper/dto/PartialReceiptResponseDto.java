package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO representing purchase orders with partially received items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartialReceiptResponseDto {

    private int totalCount;
    private List<PurchaseOrderPartialDto> purchaseOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderPartialDto {

        private Long poId;
        private String poReference;
        private SupplierSummary supplier;
        private PurchaseOrderSummary summary;
        private List<GrnSummary> grns;
        private List<PendingItem> pendingItems;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierSummary {

        private Long supplierId;
        private String supplierName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderSummary {

        private LocalDate estimatedDeliveryDate;
        private BigDecimal totalCost;
        private Integer numberOfItems;
        private String overallGrnStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrnSummary {

        private Long grnId;
        private String grnStatus;
        private LocalDateTime receivedDate;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingItem {

        private Long rawMaterialId;
        private String rawMaterialName;
        private String unitOfMeasure;
        private BigDecimal unitPrice;
        private BigDecimal requiredQty;
        private BigDecimal receivedQty;
        private BigDecimal balanceQty;
        private List<GrnReceiptBreakdown> grnBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrnReceiptBreakdown {

        private Long grnId;
        private BigDecimal receivedQty;
    }
}
