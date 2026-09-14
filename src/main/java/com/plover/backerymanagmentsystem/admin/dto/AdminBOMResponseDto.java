package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBOMResponseDto {
    private Long id;
    private ParentProductDto parentProduct;
    private String createdDate;
    private String status;
    private List<BOMChildItemDto> childItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentProductDto {
        private Long id;
        private String code;
        private String name;
        private String category;
        private String productionStage;
        private Double salePrice;
        private Double actualGP;
        private Double totalCost;
        private Double expectedGP;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BOMChildItemDto {
        private Long id;
        private String itemCode;
        private String name;
        private String type;
        private BigDecimal qty;
        private String unit;
        private String productionCenter;
        private Boolean active;
        private BigDecimal cost;
        private BigDecimal expectedGP;
        private BigDecimal actualGP;
        private BigDecimal unitPrice;
        private BigDecimal totalCost;
        private BigDecimal salePrice;
    }
}
