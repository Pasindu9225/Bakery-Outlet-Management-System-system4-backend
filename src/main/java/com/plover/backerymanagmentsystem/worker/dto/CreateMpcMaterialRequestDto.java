package com.plover.backerymanagmentsystem.worker.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMpcMaterialRequestDto {
    private Long outletId;
    private Long mpcId;
    private String notes;
    private List<Item> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        private Long rawMaterialId;
        private String rawMaterialName;
        private Double requestedQty;
        private String unitOfMeasure;
    }
}
