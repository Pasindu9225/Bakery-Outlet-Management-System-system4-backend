package com.plover.backerymanagmentsystem.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for creating a new GTN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGtnResponseDto {
    private boolean success;
    private String message;
    private Integer gtnId;
}
