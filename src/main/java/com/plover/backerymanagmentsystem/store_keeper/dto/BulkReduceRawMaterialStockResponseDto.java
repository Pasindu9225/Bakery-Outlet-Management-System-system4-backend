package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkReduceRawMaterialStockResponseDto {

    private List<ReduceRawMaterialStockResponseDto> successfulReductions;
    private List<StockReductionError> failedReductions;
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockReductionError {

        private Long materialId;
        private String materialName;
        private Double requestedQuantity;
        private String errorMessage;
        private String errorType; // "INSUFFICIENT_STOCK", "MATERIAL_NOT_FOUND", etc.
    }
}
