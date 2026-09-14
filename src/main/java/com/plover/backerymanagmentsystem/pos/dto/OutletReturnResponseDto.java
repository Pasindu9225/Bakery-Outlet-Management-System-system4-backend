package com.plover.backerymanagmentsystem.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletReturnResponseDto {
    private Boolean success;
    private String message;
    private Long returnId;
    private String returnNoteId;
}
