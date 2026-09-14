package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialWithBatchesDto {

    private Long id;
    private String code;
    private String materialName;
    private String name;
    private String brand;
    private String genericMaterialName;
    private String category;
    private String unit;
    private List<RawMaterialBatchDto> batches;
    private Double totalQuantity;
    private Double minQty;
    private String expireDate;
    private Double unitCost;
}


