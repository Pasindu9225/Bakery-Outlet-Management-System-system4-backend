package com.plover.backerymanagmentsystem.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosTableResponseDto {
    private Long id;
    private String tableName;
    private String status;
    private Integer seatCount;
}
