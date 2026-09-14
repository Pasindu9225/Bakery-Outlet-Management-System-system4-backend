package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for GRN receive response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrnReceiveResponseDto {

    private Long grnId;
    private Long poId;
    private Long supplierId;
    private GrnStatus grnStatus;
    private Boolean isReceived;
    private LocalDateTime receivedDate;
    private String invoiceNumber;
    private BigDecimal total;

    private List<GrnItemUpdateDto> updatedItems;
    private List<StockUpdateDto> stockUpdates;
    private List<String> batchCreationResults;
    private List<String> errors;

    private String message;
    private Boolean success;

    /**
     * DTO for updated GRN item information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrnItemUpdateDto {

        private Long grnItemId;
        private Long rawMaterialId;
        private String rawMaterialName;
        private BigDecimal previousReceivedQuantity;
        private BigDecimal newReceivedQuantity;
        private String uom;
    }

    /**
     * DTO for raw material stock update information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockUpdateDto {

        private Long rawMaterialId;
        private String rawMaterialName;
        private String materialCode;
        private Double previousStock;
        private BigDecimal addedQuantity;
        private Double newStock;
        private String unitOfMeasure;
        private Boolean belowMinimumLevel;
    }
}
