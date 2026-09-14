package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkReduceRawMaterialStockRequestDto {

    @NotEmpty(message = "At least one material stock reduction is required")
    @Valid
    private List<ReduceRawMaterialStockRequestDto> materialReductions;
}
