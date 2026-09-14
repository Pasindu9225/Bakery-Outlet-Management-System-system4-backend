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
 * DTO for purchase order creation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseOrderResponseDto {

    private Long poId;
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalCost;
    private Integer numberOfItems;
    private LocalDate estimatedDeliveryDate;
    private String status;
    private List<PurchaseOrderItemResponseDto> items;
    private BigDecimal totalEstimatedCost;
    private BigDecimal costVariance;
    private Double costVariancePercentage;
    private LocalDateTime createdAt;

    /**
     * Nested DTO for purchase order item response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderItemResponseDto {

        private Long poiId;
        private Long rawMaterialId;
        private String rawMaterialName;
        private Double requiredQty;
        private String unitOfMeasure;
        private BigDecimal actualCost;
        private BigDecimal estimatedCost;
        private BigDecimal totalActualCost;
        private BigDecimal totalEstimatedCost;
    }
}
