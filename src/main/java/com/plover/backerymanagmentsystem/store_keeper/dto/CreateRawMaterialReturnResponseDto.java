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
 * Response DTO for raw material return creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRawMaterialReturnResponseDto {

    private Long returnId;
    private LocalDate returnDate;
    private Integer numberOfItems;
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalCost;
    private List<ReturnItemResponseDto> returnItems;
    private LocalDateTime createdAt;

    /**
     * Nested DTO for individual return item response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemResponseDto {

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
        private String brand;
        private String batchNo;
        private LocalDate expiryDate;
        private Double availableStock;
    }
}
