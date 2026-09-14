package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for approving raw material return items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRawMaterialReturnRequestDto {

    @NotEmpty(message = "Return item IDs list cannot be empty")
    private List<@NotNull @Positive Long> returnItemIds;
}
