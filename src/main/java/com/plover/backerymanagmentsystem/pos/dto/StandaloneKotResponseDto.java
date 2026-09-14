package com.plover.backerymanagmentsystem.pos.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StandaloneKotResponseDto {
    private Long kotId;
    private String orderNumber;
    private String status;
}
