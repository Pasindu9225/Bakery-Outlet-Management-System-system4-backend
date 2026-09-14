package com.plover.backerymanagmentsystem.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the item return operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponseDto {

    private boolean success;
    private String message;
    private Integer returnId;
}
