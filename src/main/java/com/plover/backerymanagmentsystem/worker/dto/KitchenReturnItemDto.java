package com.plover.backerymanagmentsystem.worker.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KitchenReturnItemDto {
    private Long id;
    private Long rawMaterialId;
    private String rawMaterialName;
    private Double quantity;
    private String unit;
    private String reason;
}
