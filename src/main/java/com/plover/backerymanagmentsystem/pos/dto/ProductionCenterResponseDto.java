package com.plover.backerymanagmentsystem.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionCenterResponseDto {
    private Long id;
    private String centerName;
}
