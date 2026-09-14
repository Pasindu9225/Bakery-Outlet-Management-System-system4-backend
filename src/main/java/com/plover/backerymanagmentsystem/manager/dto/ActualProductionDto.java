package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualProductionDto {
    private Long id;
    private Long productId;
    private String productName;
    private Integer availableQuantity;
    private OffsetDateTime lastUpdated;
}
