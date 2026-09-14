package com.plover.backerymanagmentsystem.manager.dto;

import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem.ReturnStatus;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReturnItemStatusRequestDto {

    @NotNull(message = "Return ID is required")
    private Long returnId;

    @NotEmpty(message = "At least one item must be provided")
    private List<ItemStatusUpdate> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemStatusUpdate {
        @NotNull(message = "Item ID is required")
        private Long itemId;

        @NotNull(message = "Status is required")
        private ReturnStatus status;
    }
}


