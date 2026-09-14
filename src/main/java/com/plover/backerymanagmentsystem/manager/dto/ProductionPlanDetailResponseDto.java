package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanDetailResponseDto {

    private String id;
    private String requestName;
    private String requestDate;
    private String status;
    private List<ProductDetailDto> products;
    private List<RawMaterialDetailDto> rawMaterials;
    private String createdBy;
    private String createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDetailDto {
        private String name;
        private Integer quantity;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawMaterialDetailDto {
        private String name;
        private Double quantity;
        private String unit;
    }
}
