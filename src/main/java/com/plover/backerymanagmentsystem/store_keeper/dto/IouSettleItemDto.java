package com.plover.backerymanagmentsystem.store_keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IouSettleItemDto {
    private Long iouRequestItemId; // ID of the existing item
    private Long rawMaterialId;
    private Double actualQuantity;
    private Double actualPrice;
    private String actualItemName;
    private String actualSupplierName;
    private String unitOfMeasure;
}
