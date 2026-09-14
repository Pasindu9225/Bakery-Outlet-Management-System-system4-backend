package com.plover.backerymanagmentsystem.worker.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IngredientRequestItemDto {
    private Long id;
    private Long rawMaterialId;
    private String rawMaterialName;
    private Double requestedQty;
    private Double issuedQty;
    private Double availableQty;
    private String unitOfMeasure;
}
