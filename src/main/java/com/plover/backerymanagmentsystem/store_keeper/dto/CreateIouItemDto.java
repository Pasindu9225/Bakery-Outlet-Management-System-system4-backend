package com.plover.backerymanagmentsystem.store_keeper.dto;

import com.plover.backerymanagmentsystem.store_keeper.model.IouItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIouItemDto {
    private String supplierName;
    private String supplierContact;
    private IouItemType itemType;
    private Long rawMaterialId;
    private String itemName;
    private String unitOfMeasure;
    private Double estimatedQuantity;
    private Double estimatedPrice;
}
