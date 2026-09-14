package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for retrieving raw materials in a specific purchase order.
 * Contains purchase order details and associated raw materials with item
 * details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRawMaterialsResponseDto {

    private Long purchaseOrderId;
    private BigDecimal totalCost;
    private Integer numberOfItems;
    private LocalDate estimatedDeliveryDate;
    private Long supplierId;
    private String supplierName;
    private List<PurchaseOrderRawMaterialDto> rawMaterials;

    /**
     * DTO representing raw material information in a purchase order.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderRawMaterialDto {

        // Raw Material Info
        private Long rawMaterialId;
        private String rawMaterialName;
        private Double currentStock;
        private LocalDate expireDate;

        // Purchase Order Item Details
        private Integer requiredQty;
        private Integer receivedQty;
        private BigDecimal actualCost;
        private BigDecimal estimatedCost;
        private String unitOfMeasure;

        // Related GRN item id (if a GRN exists for this PO and material)
        private Long grnItemId;
        private String batchNo;
        private LocalDate grnExpireDate;
    }
}
