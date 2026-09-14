package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniStoreDailyInventoryResponse {

    private Long outletId;
    private LocalDate asOfDate;
    private List<MiniStoreInventoryDetails> miniStores;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniStoreInventoryDetails {

        private Integer miniStoreId;
        private String miniStoreName;
        private List<MiniStoreRawMaterialItem> rawMaterials;
        private List<MiniStoreProductItem> products;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniStoreRawMaterialItem {

        private Integer itemId;
        private String name;
        private Long rawMaterialId;
        private String materialName;
        private String materialCode;
        private String unitOfMeasure;
        private BigDecimal systemQuantity;
        private BigDecimal physicalQuantity;
        private BigDecimal variance;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniStoreProductItem {

        private Integer itemId;
        private String name;
        private Long productId;
        private String productName;
        private String productCode;
        private Double unitPrice;
        private BigDecimal systemQuantity;
        private BigDecimal physicalQuantity;
        private BigDecimal variance;
        private LocalDateTime updatedAt;
    }
}
