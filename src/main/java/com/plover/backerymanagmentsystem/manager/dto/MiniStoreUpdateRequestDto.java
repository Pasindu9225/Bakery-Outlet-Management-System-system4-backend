package com.plover.backerymanagmentsystem.manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniStoreUpdateRequestDto {
    
    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<MiniStoreItemDto> items;
}
