package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem.ReturnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.ReturnReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for fetching all raw material returns with item details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAllRawMaterialReturnsResponseDto {

    private Integer totalCount;
    private List<ReturnSummaryDto> returns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnSummaryDto {
        private Long returnId;
        private LocalDate returnDate;
        private Integer numberOfItems;
        private Long supplierId;
        private String supplierName;
        private BigDecimal totalCost;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ReturnItemDto> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDto {
        private Long returnItemId;
        private Long rawMaterialId;
        private String rawMaterialName;
        private String category;
        private String unitOfMeasure;
        private Double returnQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private ReturnStatus status;
        private ReturnReason reason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String brand;
        private String batchNo;
        private LocalDate expiryDate;
        private Double availableStock;
    }
}


