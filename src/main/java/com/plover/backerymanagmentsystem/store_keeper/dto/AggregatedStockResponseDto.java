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
public class AggregatedStockResponseDto {
    private Long id; // Generic Material ID

    @com.fasterxml.jackson.annotation.JsonProperty("genericMaterialName")
    private String name; // Generic Material Name
    private String category;
    private String unitOfMeasure;
    private Double totalStock;
    private List<BrandStockSummary> brands;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrandStockSummary {
        private String brandName;
        private Double stock;
    }
}
