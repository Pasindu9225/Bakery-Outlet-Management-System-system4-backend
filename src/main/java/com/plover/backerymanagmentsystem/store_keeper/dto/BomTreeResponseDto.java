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
public class BomTreeResponseDto {

    private Long productId;
    private String productName;
    private List<BomNodeDto> children;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BomNodeDto {
        private Long childItemId;
        private String childName;
        private String childType; // product | raw_material
        private java.math.BigDecimal quantity;
        private String unit;
        private String productionCenter; // name
        private List<BomNodeDto> children;
    }
}


