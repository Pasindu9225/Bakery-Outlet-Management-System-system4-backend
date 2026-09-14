package com.plover.backerymanagmentsystem.worker.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransferNoteItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private String unit;
}
