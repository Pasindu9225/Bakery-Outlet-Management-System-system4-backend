package com.plover.backerymanagmentsystem.worker.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateMpcMaterialRequestDto {
    private Long outletId;
    private Long mpcId;
    private String notes;
    private List<ProductLine> products;
    private List<RawMaterialLine> rawMaterials;
    private List<RawMaterialLine> items;

    public List<RawMaterialLine> getRawMaterials() {
        if (rawMaterials != null && !rawMaterials.isEmpty()) {
            return rawMaterials;
        }
        return items;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductLine {
        private Long productId;
        private Integer plates;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RawMaterialLine {
        private Long rawMaterialId;
        private String rawMaterialName;
        private Double requestedQty;
        private Double qty;
        private String unitOfMeasure;
        private String unit;

        public RawMaterialLine(Long rawMaterialId, Double qty) {
            this.rawMaterialId = rawMaterialId;
            this.qty = qty;
        }

        public Double getQty() {
            return qty != null ? qty : requestedQty;
        }
    }
}
