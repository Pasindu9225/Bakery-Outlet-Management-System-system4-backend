package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBOMRequestDto {
    private Long parentProductId;
    private List<BOMItemRequestDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BOMItemRequestDto {
        private Long childItemId;
        private String childType; // "raw_material" or "product"
        private BigDecimal quantity;
        private String unit;
        private Long productionCenterId;
        private Boolean isActive;
    }
}
